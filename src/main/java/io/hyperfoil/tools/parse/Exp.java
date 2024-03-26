package io.hyperfoil.tools.parse;

import io.hyperfoil.tools.parse.internal.CheatChars;
import io.hyperfoil.tools.parse.internal.DropString;
import io.hyperfoil.tools.parse.internal.IMatcher;
import io.hyperfoil.tools.parse.internal.RegexMatcher;
import io.hyperfoil.tools.parse.json.JsonBuilder;
import io.hyperfoil.tools.yaup.HashedLists;
import io.hyperfoil.tools.yaup.StringUtil;
import io.hyperfoil.tools.yaup.json.Json;
import org.jboss.logging.Logger;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

public class Exp {

   final static Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

   public Json toJson(){
      Json rtrn = new Json();
      rtrn.set("pattern",getPattern());
      rtrn.set("eat",Eat.from(getEat()) == Eat.Width ? getEat() : Eat.from(getEat()));
      rtrn.set("name",getName());
      rtrn.set("range",getRange());
      if(getNest()!=null){
         rtrn.set("nest",getNest());
      }
      if(!getRequires().isEmpty()){
         rtrn.set("requires",getRequires());
      }
      if(!getEnables().isEmpty()){
         rtrn.set("enables",getEnables());
      }
      if(!getDisables().isEmpty()){
         rtrn.set("disables",getDisables());
      }
      if(!getWith().isEmpty()){
         Json with = new Json(false);
         getWith().forEach((k,v)->{
            with.set(k,v);
         });
         rtrn.set("with",with);
      }

      if(!callbacks.isEmpty()){
         Json execute = new Json();
         callbacks.forEach(callback->{
            if(callback instanceof JsMatchAction){
               JsMatchAction matchAction = (JsMatchAction)callback;
               execute.add(matchAction.getJs());
            }
         });
         if(!execute.isEmpty()){
            if(execute.size()==1){
              rtrn.set("execute",execute.get(0));
            } else {
               rtrn.set("execute", execute);
            }
         }
      }




      return rtrn;
   }
   public static Exp fromJson(Json json){
      if(!json.has("pattern") || !(json.get("pattern") instanceof String)){
         throw new IllegalArgumentException("exp requires a pattern");
      }

      String name = json.getString("name",json.getString("pattern"));
      Exp rtrn = new Exp(name,json.getString("pattern"));
      if(json.has("eat")){
         rtrn.eat(Eat.from(json.get("eat").toString()));
      }
      if(json.has("range")){
         rtrn.setRange(StringUtil.getEnum(json.getString("range"),MatchRange.class,MatchRange.AfterParent));
      }
      if(json.has("nest")){
         rtrn.nest(json.getString("nest"));
      }
      if(json.has("requires")){
         Object value = json.get("requires");
         if(value instanceof String){
            rtrn.requires(value.toString());
         }else if (value instanceof Json){
            ((Json)value).forEach(entry->rtrn.requires(entry.toString()));
         }
      }
      if(json.has("enables")){
         Object value = json.get("enables");
         if(value instanceof String){
            rtrn.enables(value.toString());
         }else if (value instanceof Json){
            ((Json)value).forEach(entry->rtrn.enables(entry.toString()));
         }
      }
      if(json.has("disables")){
         Object value = json.get("disables");
         if(value instanceof String){
            rtrn.disables(value.toString());
         }else if (value instanceof Json){
            ((Json)value).forEach(entry->rtrn.disables(entry.toString()));
         }
      }
      rtrn.setMerge(StringUtil.getEnum(json.getString("merge",""),ExpMerge.class,ExpMerge.ByKey));
      if(json.has("with")){
         Object value = json.get("with");
         if(value instanceof Json){
            ((Json)value).forEach((k,v)->{
               rtrn.with(k.toString(),v);
            });
         }else{
            throw new IllegalArgumentException("unsupported with :"+value.getClass().getSimpleName()+" "+value.toString());
         }
      }
      if(json.has("rules")){
         Object value= json.get("rules");
         if(value instanceof Json){
            ((Json)value).forEach(rule->{
               if(rule instanceof String){
                  ExpRule newRule = StringUtil.getEnum(rule.toString(),ExpRule.class,null);
                  if(newRule!=null) {
                     rtrn.addRule(newRule);
                  }else{
                     throw new IllegalArgumentException("failed to parse rule from "+rule.toString());
                  }
               }else if (rule instanceof Json && ((Json)rule).size()==1 && !((Json)rule).isArray()){
                  ((Json)rule).forEach((k,v)->{
                     ExpRule newRule = StringUtil.getEnum(k.toString(),ExpRule.class,null);
                     if(newRule != null){
                        rtrn.addRule(newRule,v);
                     }else{
                        throw new IllegalArgumentException("failed to parse rule from "+k.toString());
                     }
                  });
               }else{
                  throw new IllegalArgumentException("could not create rule form "+rule.getClass().getSimpleName()+" "+rule);
               }
            });
         }
      }
      if(json.has("fields")){
         json.getJson("fields").forEach((fieldName,fieldValue)->{
            if(fieldValue instanceof Json){
               Json valueJson = (Json)fieldValue;
               ValueType type = StringUtil.getEnum(valueJson.getString("type",""),ValueType.class,ValueType.Auto);
               ValueMerge merge = StringUtil.getEnum(valueJson.getString("merge",""),ValueMerge.class,ValueMerge.Auto);
               rtrn.setType(fieldName.toString(),type);
               if(valueJson.has("target") && ValueMerge.Key.equals(merge)){
                  rtrn.setKeyValue(fieldName.toString(),valueJson.getString("target"));
               }else{
                  rtrn.setMerge(fieldName.toString(),merge);
               }
            }else{
               throw new IllegalArgumentException(fieldName+" value is not json "+fieldValue);
            }
         });
      }
      if(json.has("execute")){
         Object value = json.get("execute");
         if(value instanceof String){
            rtrn.execute(new JsMatchAction(value.toString()));
         }else if (value instanceof Json && ((Json)value).isArray()){
            ((Json)value).forEach(entry->{
               rtrn.execute(new JsMatchAction(entry.toString()));
            });
         }
      }
      if(json.has("children")){
         json.getJson("children").forEach(child->{
            if(child instanceof String){
               rtrn.add(new Exp(child.toString()));
            }else if (child instanceof Json && !((Json)child).isArray()){
               rtrn.add(fromJson((Json)child));
            }else{
               throw new IllegalArgumentException("could not create child Exp from "+child);
            }
         });
      }
      return rtrn;
   };

   public static Json getSchema(){
      Json rtrn = new Json();
      rtrn.set("$schema","http://json-schema.org/draft-07/schema");
      rtrn.set("definitions",new Json());
      rtrn.getJson("definitions").set("exp",getSchemaDefinition("exp"));
      rtrn.set("$ref","#/definitions/exp");
      return rtrn;
   }
   public static Json getSchemaDefinition(String name){
      return Json.fromJs("" +
         "{" +
         "  oneOf: [" +
         "    {type: 'string'}," +
         "    {" +
         "      type: 'object'," +
         "      properties: {" +
         "        name: { type: 'string' }," +
         "        pattern: { type: 'string' }," +
         "        eat: { oneOf: [ { type: 'number' }, { enum: ['None','Match','ToMatch','Line'] } ] }," +
         "        range: { enum: ['EntireLine','AfterParent','BeforeParent'] }," +
         "        nest: { type: 'string' }," +
         "        requires: { oneOf: [ { type: 'string'}, { type: 'array', items: { type: 'string' } } ] }," +
         "        enables: { oneOf: [ { type: 'string'}, { type: 'array', items: { type: 'string' } } ] }," +
         "        disables: { oneOf: [ { type: 'string'}, { type: 'array', items: { type: 'string' } } ] }," +
         "        merge: { enum: ['ByKey','AsEntry','Extend'] }," +
         "        with: { type: 'object' }," +
         "        rules: {" +
         "          type: 'array'," +
         "          items: {" +
         "            oneOf: [" +
         "              { enum: ['Repeat','RepeatChildren','PushTarget','PreClose','PostClose','PrePopTarget','PostPopTarget','PreClearTarget','PostClearTarget','TargetRoot'] }," +
         "              {" +
         "                type: 'object'," +
         "                properties: {" +
         "                  PushTarget: { oneOf: [ {type: 'string'} , { type: 'array', items: { type: 'string' } } ] }," +
         "                  PreClearTarget: { oneOf: [ {type: 'string'} , { type: 'array', items: { type: 'string' } } ] }," +
         "                  PostClearTarget: { oneOf: [ {type: 'string'} , { type: 'array', items: { type: 'string' } } ] }," +
         "                  PreClose: { oneOf: [ {type: 'string'} , { type: 'array', items: { type: 'string' } } ] }," +
         "                  PostClose: { oneOf: [ {type: 'string'} , { type: 'array', items: { type: 'string' } } ] }," +
         "                  TargetRoot: { oneOf: [ {type: 'string'} , { type: 'array', items: { type: 'string' } } ] }" +
         "                }" +
         "              }" +
         "            ]" +
         "          }" +
         "        }," +
         "        fields: { " +
         "          type: 'object'," +
         "          additionalProperties: {" +
         "            type: 'object'," +
         "            properties: {" +
         "              type: { enum: ['Auto','String','KMG','Integer','Decimal','Json'] }," +
         "              merge: { enum: ['Auto','BooleanKey','BooleanValue','TargetId','Count','Add','List','Key','Set','First','Last','TreeSibling','TreeMerging'] }," +
         "            }," +
         "            if: { properties: { merge: {enum: ['Key']} } }," +
         "            then: { required: ['target'] }" +
         "          }" +
         "        }," +
         "        execute: {oneOf: [ {type: 'string'}, {type: 'array',items: { type: 'string'} } ] }," +
         "        children: { type: 'array', items: {$ref: '#/definitions/"+name+"' } }" +
         "      }," +
         "      required: ['pattern']," +
         "      additionalProperties: false" +
         "    }" +
         "  ]" +
         "}" +
         "");
   }

   static final String NEST_ARRAY = "_array";
   static final String NEST_VALUE = "_value";

   public static final String NEST_KEY_PREFIX = "${{";
   public static final String NEST_KEY_SUFFIX = "}}";
   public static final String NEST_EXTEND_PREFIX = "$[[";
   public static final String NEST_EXTEND_SUFFIX = "]]";
   public static final String CAPTURE_PREFIX = "(?<";
   public static final String CAPTURE_SUFFIX = ">";
   public static final String CAPTURE_GROUP_PATTERN = "\\(\\?<(?!!)([^>]+)>";
   public static final String ROOT_TARGET_NAME = "_ROOT";
   public static final String GROUPED_NAME = "_GROUPED";


   /**
      Stores the merge info for each value in the pattern
    */
   public static class ValueInfo {

      final String name;
      String target;
      ValueType type;
      ValueMerge merge;
      boolean skip = false;

      private ValueInfo(String name, String target, ValueType type, ValueMerge merge) {
         this.name = name;
         this.target = target;
         this.type = type;
         this.merge = merge;
      }
      public boolean isSkip(){return skip;}
      public void setSkip(boolean skip){
         this.skip = skip;
      }
      public String getName() {
         return name;
      }
      public String getTarget() {
         return target;
      }
      public void setTarget(String target){
         this.target = target;
      }

      public ValueType getType() {
         return type;
      }
      public void setType(ValueType type){
         this.type = type;
      }

      public ValueMerge getMerge() {
         return merge;
      }
      public void setMerge(ValueMerge merge){
         this.merge = merge;
      }
   }

   // How the nest token works
   public enum NestType {Extend,Field,Name}
   // the token word along with how the nest works
   private class NestPair{

      final String value;
      final NestType type;

      private NestPair(String value, NestType type) {
         this.value = value;
         this.type = type;
      }
      public String getValue() {
         return value;
      }

      public NestType getType() {
         return type;
      }
   }

   /**
    * Removes anything in the capture group name after a :
    */
   public static String removePatternValues(String pattern) {
      String rtrn = pattern;
      Matcher fieldMatcher = java.util.regex.Pattern.compile(CAPTURE_GROUP_PATTERN).matcher(pattern);
      fieldMatcher.reset(pattern);
      while(fieldMatcher.find()){
         String match = fieldMatcher.group(1);
         if(match.indexOf(":") > -1){
            rtrn = rtrn.replace(match,match.substring(0,match.indexOf(":")));
         }
      }
      return rtrn;
   }

   /**
    * Identify the capture groups and any value information from the pattern
    */
   public static Map<String,ValueInfo> parsePattern(String pattern){
      Matcher fieldMatcher = java.util.regex.Pattern.compile(CAPTURE_GROUP_PATTERN).matcher(pattern);
      Map<String,ValueInfo> rtrn = new LinkedHashMap<>();

      while(fieldMatcher.find()){
         Queue<String> fieldKeys = new LinkedList<>(Arrays.asList(fieldMatcher.group(1).split(":")));
         String name = fieldKeys.poll();
         ValueInfo valueInfo = rtrn.containsKey(name) ? rtrn.get(name) : new ValueInfo(name,null,ValueType.Auto,ValueMerge.Auto);
         rtrn.put(name,valueInfo);
         while(!fieldKeys.isEmpty()){
            String key = fieldKeys.poll().toLowerCase();
            if(StringUtil.getEnum(key,ValueType.class)!=null){
               valueInfo.setType(StringUtil.getEnum(key,ValueType.class));
            }else if (StringUtil.getEnum(key,ValueMerge.class)!=null){
               valueInfo.setMerge(StringUtil.getEnum(key,ValueMerge.class));
            }else if(key.contains("=")){
               String type = key.substring(0,key.indexOf("="));
               String value = key.substring(key.indexOf("=")+1);
               //changed from ValueType, why was it as ValueType?
               ValueMerge valueMerge = StringUtil.getEnum(type,ValueMerge.class);
               if(valueMerge!=null){
                  valueInfo.setMerge(valueMerge);
                  valueInfo.setTarget(value);
                  if(ValueMerge.Key.equals(valueMerge)){
                     if(!rtrn.containsKey(value)){
                        rtrn.put(value,new ValueInfo(value,null,ValueType.Auto,ValueMerge.Auto));
                     }
                     rtrn.get(value).setSkip(true);
                  }else {
                  }
               }else{
                  //TODO log the error for invalid type?
               }
            }else{
               logger.error("what is a "+key);
               throw new IllegalArgumentException("cannot infer type info from "+key+" in "+fieldMatcher.group(1)+" of "+pattern);
            }
         }
      }
      return rtrn;
   }
   public String buildPattern(){
      String rtrn = getPattern();
      for(String key : fields.keySet()){
         ValueInfo info = fields.get(key);
         String replacement = key;
         if(!info.getType().equals(ValueType.Auto)){
            replacement+=":"+info.getType().toString().toLowerCase();
         }
         if(!info.getMerge().equals(ValueMerge.Auto)){
            replacement+=":"+info.getMerge().toString().toLowerCase();
            if(ValueMerge.Key.equals(info.getMerge())){
               replacement+="="+info.getTarget();
            }
         }
         int start = rtrn.indexOf(CAPTURE_PREFIX+key);
         int stop = rtrn.indexOf(CAPTURE_SUFFIX,start);
         String substring = rtrn.substring(start,stop+CAPTURE_SUFFIX.length());
         rtrn = rtrn.replace(substring,CAPTURE_PREFIX+replacement+CAPTURE_SUFFIX);
      }
      return rtrn;
   }

   private LinkedList<MatchAction> callbacks = new LinkedList<>();

   private LinkedHashMap<String,Object> with = new LinkedHashMap<>();

   private LinkedList<Exp> children = new LinkedList<>();
   private HashedLists<ExpRule,Object> rules = new HashedLists<>();

   private LinkedHashSet<String> enables = new LinkedHashSet<>();
   private LinkedHashSet<String> disables = new LinkedHashSet<>();
   private LinkedHashSet<String> requires = new LinkedHashSet<>();

   private Map<String,ValueInfo> fields;
   private LinkedList<NestPair> nesting = new LinkedList<>();

   private MatchRange matchRange = MatchRange.AfterParent;
   private ExpMerge expMerge = ExpMerge.ByKey;

   private int eat = Eat.Match.getId();

   final private String name;
   final private String pattern;
   final private IMatcher matcher;

   private boolean debug = false;

   public Exp(String pattern){
      this(pattern,pattern);
   }
   public Exp(String name, String pattern){
      if(name == null || pattern == null){
         throw new IllegalArgumentException("name and pattern cannot be null");
      }
      this.name = name;
      this.pattern = pattern;
      this.fields = parsePattern(pattern);

      String safePattern = removePatternValues(pattern);
      this.matcher = new RegexMatcher(safePattern);
   }

   public boolean isDebug(){return debug;}
   public Exp debug(){
      return debug(true);
   }
   public Exp debug(boolean debug){
      this.debug = debug;
      return this;
   }

   public String getPattern(){return pattern;}
   public int getEat(){return eat;}

   public Exp with(String key, Object value){
      if(key == null || value == null){
         throw new IllegalArgumentException("key and value cannot be null");
      }
      with.putIfAbsent(key,value);
      return this;
   }
   public Map<String,Object> getWith(){return with;}
   public boolean isNested(){
      return !nesting.isEmpty();
   }
   public ExpMerge getExpMerge(){return expMerge;}
   public void eachNest(BiConsumer<String,NestType> action){
      nesting.forEach(np->action.accept(np.value,np.type));
   }
   public boolean hasRules(){
      return !rules.isEmpty();
   }
   public void eachRule(BiConsumer<ExpRule,List<Object>> action){
      rules.forEach(action);
   }

   public boolean isTargeting(){
      return fields.values().stream()
         .map(valueInfo -> valueInfo.merge.isTargeting())
         .reduce(Boolean::logicalOr).orElse(false);
   }

   public Exp nest(String nesting){
      if(nesting == null){
         throw new IllegalArgumentException("nesting cannot be null");
      }
      List<String> nests = Json.dotChain(nesting);
      for(String nest : nests){
         int index=-1;
         if(nest.startsWith(NEST_KEY_PREFIX) && nest.endsWith(NEST_KEY_SUFFIX)){
            String fieldName = nest.substring(NEST_KEY_PREFIX.length(),nest.length()-NEST_KEY_SUFFIX.length());
            key(fieldName);
         }else if (nest.startsWith(NEST_EXTEND_PREFIX) && nest.endsWith(NEST_EXTEND_SUFFIX)){
            String extendName = nest.substring(NEST_EXTEND_PREFIX.length(),nest.length()-NEST_EXTEND_SUFFIX.length());
            extend(extendName);
         }else{
            group(nest);
         }
      }
      return this;
   }
   public String getNest(){

      if(!isNested()){
         return "";
      }
      StringBuilder builder = new StringBuilder();
      this.eachNest((key,type)->{
         if(key.contains(".")){
            key = key.replace(".","\\.");
         }
         if(builder.length() > 0){
            builder.append(".");
         }
         if(Exp.NestType.Field.equals(type)){
            builder.append(NEST_KEY_PREFIX+key+NEST_KEY_SUFFIX);
         }else if (Exp.NestType.Extend.equals(type)){
            builder.append(NEST_EXTEND_PREFIX+key+NEST_EXTEND_SUFFIX);
         }else{
            builder.append(key);
         }
      });
      return builder.toString();
   }
   public Exp group(String name){
      if(name == null){
         throw new IllegalArgumentException("name cannot be null");
      }
      nesting.add(new NestPair(name,NestType.Name));
      return this;
   }
   public Exp key(String name){
      if(name == null){
         throw new IllegalArgumentException("name cannot be null");
      }
      nesting.add(new NestPair(name,NestType.Field));
      return this;
   }
   public Exp extend(String name){
      if(name == null){
         throw new IllegalArgumentException("name cannot be null");
      }
      nesting.add(new NestPair(name,NestType.Extend));
      return this;
   }

   public Exp eat(int width){
      eat = width;
      return this;
   }
   public Exp eat(Eat toEat){
      if(toEat == null){
         throw new IllegalArgumentException("eat cannot be null");
      }
      eat = toEat.getId();
      return this;
   }
   public Exp execute(MatchAction action){
      if(action == null){
         throw new IllegalArgumentException("action cannot be null");
      }
      callbacks.add(action);
      return this;
   }
   public Exp add(Exp child){
      if(child == null){
         throw new IllegalArgumentException("child cannot be null");
      }
      children.add(child);
      return this;
   }
   public boolean hasChildren(){return !children.isEmpty();}
   public void eachChild(Consumer<Exp> consumer){
      children.forEach(consumer);
   }
   public Exp setType(String fieldName, ValueType type){
      if(fieldName == null || type == null){
         throw new IllegalArgumentException("fieldName and type cannot be null");
      }
      fields.get(fieldName).setType(type);
      return this;
   }
   public Exp setKeyValue(String key, String value){
      if(key == null || value == null){
         throw new IllegalArgumentException("key and value cannot be null");
      }
      fields.get(key).setMerge(ValueMerge.Key);
      fields.get(key).setTarget(value);
      fields.get(value).setSkip(true);
      return this;
   }
   public Exp setMerge(String fieldName, ValueMerge merge){
      if(fieldName == null || merge == null){
         throw new IllegalArgumentException("fieldName and merge cannot be null");
      }

      fields.get(fieldName).setMerge(merge);
      return this;
   }
   public MatchRange getRange(){return matchRange;}
   public Exp setRange(MatchRange range){
      if(range == null){
         throw new IllegalArgumentException("range cannot be null");
      }

      this.matchRange = range;
      return this;
   }
   public Exp addRule(ExpRule rule){
      if(rule == null){
         throw new IllegalArgumentException("rule cannot be null");
      }
      this.rules.put(rule,null);
      return this;
   }
   public Exp addRule(ExpRule rule, Object value){
      if(rule == null || value == null){
         throw new IllegalArgumentException("rule and value cannot be null");
      }

      this.rules.put(rule,value);
      return this;
   }

   public Exp setMerge(ExpMerge merge){
      if(merge == null){
         throw new IllegalArgumentException("merge cannot be null");
      }

      this.expMerge = merge;
      return this;
   }

   public boolean hasRule(ExpRule rule){
      return this.rules.containsKey(rule);
   }
   public Json getNestedTarget(Json currentTarget, JsonBuilder builder){
      Json returnTarget = currentTarget;
      if(nesting.isEmpty()){//for Tree
         switch (this.expMerge){
            case AsEntry:
               if(currentTarget == builder.getRoot()){
                  if (builder.getRoot().isArray()){
                     Json newEnty = new Json();
                     currentTarget.add(newEnty);
                     builder.pushTarget(newEnty);
                     currentTarget = builder.getTarget();
                  }
                  //currently do nothing
               }else if (currentTarget.isEmpty()){
                  Json newEntry = new Json();
                  currentTarget.add(newEntry);
                  builder.pushTarget(newEntry);
                  currentTarget = builder.getTarget();
               }else if (!currentTarget.isArray()){
                  if(builder.peekTarget(1) != null && builder.peekTarget(1).isArray()){
                     Json newTarget = new Json();
                     builder.popTarget();
                     builder.getTarget().add(newTarget);
                     builder.pushTarget(newTarget);
                     currentTarget = builder.getTarget();
                  }
               }
               break;

         }
      }else{
         for(Iterator<NestPair> iter = nesting.iterator(); iter.hasNext();){
            NestPair nestPair = iter.next();
            String nestValue = nestPair.getValue();
            NestType nestType = nestPair.getType();
            boolean extend = false;
            if(NestType.Field.equals(nestType)){
               nestValue = this.matcher.group(nestValue);
               if(nestValue==null || nestValue.isEmpty()){
                  throw new IllegalArgumentException("Cannot nest with "+nestValue);
               }
            }
            if(NestType.Extend.equals(nestType)){
               extend = true;
            }
            if(!iter.hasNext()){
               switch (this.expMerge){
                  case AsEntry:
                     if(returnTarget.has(nestValue)){
                        Json entry = new Json(false);
                        returnTarget.add(nestValue,entry);
                        returnTarget = entry;
                     }else{
                        Json entry = new Json(false);
                        Json arry = new Json();
                        arry.add(entry);
                        returnTarget.set(nestValue,arry);
                        returnTarget = entry;
                     }
                     break;
                  case Extend:
                     if(returnTarget.has(nestValue)){
                        Json arry = returnTarget.getJson(nestValue);
                        Json last = arry.getJson(arry.size()-1);
                        returnTarget = last;
                     }else{
                        Json entry = new Json(false);
                        Json arry = new Json();
                        arry.add(entry);
                        returnTarget.set(nestValue,arry);
                        returnTarget = entry;
                     }
                     break;
                  case ByKey:
                     if(returnTarget.has(nestValue)){
                        returnTarget = returnTarget.getJson(nestValue);
                     }else{
                        Json newJson = new Json(false);
                        returnTarget.set(nestValue,newJson);
                        returnTarget = newJson;
                     }
                  default:

                     break;
               }
            }else{
               if(returnTarget.has(nestValue)){
                  Object obj = returnTarget.get(nestValue);
                  if(obj instanceof Json && ((Json)obj).isArray()){
                     Json groupArry = (Json)obj;
                     if( extend || ExpMerge.Extend.equals(expMerge) && groupArry.size() > 0){
                        returnTarget = groupArry.getJson(groupArry.size()-1);
                     }else{
                        Json newJson = new Json(false);
                        groupArry.add(newJson);
                        returnTarget = newJson;
                     }
                  }else{
                     returnTarget = returnTarget.getJson(nestValue);
                  }
               }else{
                  Json newJson = new Json(false);
                  returnTarget.set(nestValue,newJson);
                  returnTarget = newJson;
               }
            }
         }
      }
      return returnTarget;
   }
   public Json apply(String line){
      if(line == null){
         throw new IllegalArgumentException("line cannot be null");
      }
      JsonBuilder builder = new JsonBuilder();
      apply(new CheatChars(line),builder,null);
      return builder.getRoot();
   }
   public boolean apply(DropString line, JsonBuilder builder, Parser parser){
      return apply(line,builder,parser,line.reference(0));
   }
   protected boolean apply(DropString line, JsonBuilder builder, Parser parser, DropString.Ref startIndex){
      if(isDebug() && startIndex.get() < line.length()){
         logger.debugf("%s apply to %s",getName(),line.subSequence(startIndex.get(),line.length()));
      }

      boolean rtrn = false;
      try {
         //cannot return for line.length==0 because pattern may expect empty line
         if (startIndex.get() > line.length() && this.matchRange.equals(MatchRange.AfterParent)) {

            if(isDebug()){

            }
            return false;
         }

         //check if all requires are satisfied
         if (!this.requires.isEmpty() && parser != null) {
            boolean satisfyRequired = requires.stream()
               .filter(required -> !parser.getState(required))//return true if the requirement isn't satisfied
               .findAny()//will only find missing requirements
               .orElse(null) == null;

            if (!satisfyRequired) {
               if(isDebug()){
                  logger.debugf("%s does not satisfy %s",getName(),requires.stream().filter(required -> !parser.getState(required)).collect(Collectors.toList()));
               }
               return false;
            }
         }

         //check if the pattern matches
         int matchStart = this.matchRange.apply(this.matcher, line, startIndex.get());

         if (this.matcher.find()) {
            rtrn = true;

            DropString.Ref firstStart = line.reference(matcher.start());
            DropString.Ref firstEnd = line.reference(matcher.end());
            int originalStart = line.getOriginalIndex(matcher.start());
            int originalEnd = line.getOriginalIndex(matcher.end());

            //adding them to line causes line.drop to impact child matching
            //line.addReference(matcherStart);
            //line.addReference(matcherEnd);

            //run the pre-populate rules
            this.rules.forEach((rule, roleObjects) -> {
               boolean changed = rule.prePopulate(builder, roleObjects);
               if(changed){
                  if(isDebug()){
                     logger.debugf("%s pre-populated target json due to %s",getName(),rule);
                  }
               }
            });

            Json startTarget = builder.getTarget();
            Json currentTarget = startTarget;

            do {//repeat this

               if(isDebug()){
                  logger.debugf("%s matches %s of %s",
                     getName(),
                     line.subSequence(matcher.start(),matcher.end()),
                     line.subSequence(startIndex.get(),line.length())
                  );
               }

               DropString.Ref matcherStart = line.reference(matcher.start());
               DropString.Ref matcherEnd = line.reference(matcher.end());


               boolean needPop = false;
               boolean populateChangedTarget = false;
               currentTarget = getNestedTarget(startTarget, builder);
               if (currentTarget != startTarget) {//nesting pushes a temporary target
                  builder.pushTarget(currentTarget, getName() + GROUPED_NAME);
                  needPop = true;

                  if(isDebug()){
                     logger.debugf("%s created a nested target json",getName());
                  }

               }

               populate(builder);
               if (currentTarget != builder.getTarget()) {//populating changed the target
                  currentTarget = builder.getTarget();
                  populateChangedTarget = true;
                  if(isDebug()){
                     logger.debugf("%s changed target json when populated pattern values",getName());

                  }
               }
               if(isDebug()){
                  logger.debugf("%s post populate json%n%s",getName(),builder.getRoot().toString(2));
               }

               DropString beforeMatch = line;
               DropString.Ref beforeMatchStart = matcherStart;
               DropString.Ref beforeMatchEnd = matcherEnd;
               if (hasChildren() &&
                  children.stream()
                     .filter(child -> MatchRange.BeforeParent.equals(child.matchRange))
                     .findAny().orElse(null) != null) {
                  //preserve this range of the line if there are children that look behind
                  //beforeMatch = new SharedString(line.getLine(),0,line.getAbsoluteIndex(matcherStart.get()),line);//only create new CheatChar if we need it
                  beforeMatch = (DropString) line.subSequence(0, matcherStart.get());
                  beforeMatchStart = beforeMatch.reference(beforeMatchStart.get());
                  beforeMatchEnd = beforeMatch.reference(beforeMatchEnd.get());
               }
               boolean preEatChanged = Eat.preEat(this.eat, line, matcher.start(), matcher.end());
               if(preEatChanged && isDebug()){
                  logger.debugf("%s changed line before children match",getName());
               }

               Json ruleTarget = currentTarget;//ugh, lambdas
               this.rules.forEach((rule, roleObjects) -> {
                  rule.preChildren(builder, ruleTarget, roleObjects);
               });
               if(ruleTarget != builder.getTarget()){
                  //the target changed?
               }
               if (!disables.isEmpty() && parser != null) {
                  disables.forEach(v -> parser.setState(v, false));
                  if(isDebug()){
                     logger.debugf("%s disabled %s",getName(),disables);
                  }
               }
               if (!enables.isEmpty() && parser != null) {
                  enables.forEach(v -> parser.setState(v, true));
                  if(isDebug()){
                     logger.debugf("%s enabled %s",getName(),enables);
                  }
               }
               int lineLength = line.length();

               if (hasChildren()) {
                  boolean childMatched = false;
                  do {
                     childMatched = false;
                     for (Exp child : children) {
                        if (MatchRange.BeforeParent.equals(child.matchRange)) {
                           boolean matched = child.apply(beforeMatch, builder, parser, beforeMatchStart);
                           childMatched = matched || childMatched;
                        } else {
                           //default is to start children from end of current match
                           boolean matched = child.apply(line, builder, parser, beforeMatchEnd);//startIndex?
                           if(isDebug()){
                              logger.debugf("%s applied child %s matched=%s",getName(),child.getName(),matched);
                           }
                           childMatched = matched || childMatched;
                        }
                     }
                  } while (childMatched && hasRule(ExpRule.RepeatChildren));
               }

               if (needPop) {
                  builder.popTarget(getName() + GROUPED_NAME);
               }

               if (line.length() != lineLength) {//reset matcher if children modified the line
                  matcher.reset(line);
                  if(isDebug()){
                     logger.debugf("%s children modified line",getName());
                  }
               }
               //default is to loop from end of current match
               //TODO how does this work if the Exp is EntireLine and doesn't eat? Is that supported?
               this.matchRange.apply(this.matcher, line, matcherEnd.get());

            } while (hasRule(ExpRule.Repeat) && this.matcher.find());
            if (rtrn) {//TODO also support pre-child match actions and call this postMatch?
               for (MatchAction action : callbacks) {
                  action.onMatch(line.getLine(), currentTarget, this /*TODO matchAction Exp*/, parser);
               }
            }

            //eat
            Eat.postEat(this.eat, line, firstStart.get(), firstEnd.get());

            //run the post-children rules
            Json ruleTarget = currentTarget;//ugh, lambdas
            this.rules.forEach((rule, roleObjects) -> {
               rule.postChildren(builder, ruleTarget, roleObjects);
            });
         }
      }catch(Exception e){
         throw new RuntimeException((e));
      }

      return rtrn;
   }
   public void populate(JsonBuilder builder){
      //first do all targeting changes
      for(ValueInfo valueInfo : fields.values()){

         if(valueInfo.isSkip()){
            continue;
         }
         if(valueInfo.getMerge().isTargeting()){
            String key = valueInfo.getName();
            Object value = valueInfo.type.apply(matcher.group(key));
            valueInfo.getMerge().merge(key,value,builder,null);
         }
      }
      //now populate values from non-targeting fields
      for(ValueInfo valueInfo : fields.values()){
         if(valueInfo.isSkip()){
            continue;
         }
         if(!valueInfo.getMerge().isTargeting()){
            String key = valueInfo.getName();
            Object target = valueInfo.getTarget();
            if(target != null){
               target = fields.get(target).type.apply(matcher.group(target.toString()));
            }
            Object value = valueInfo.type.apply(matcher.group(key));
            if (value!=null) {
               valueInfo.getMerge().merge(key, value, builder, target);
            }
         }
      }
      if(!with.isEmpty()){
         for(String key : with.keySet()){
            Json.chainSet(builder.getTarget(),key,with.get(key));
         }
      }
   }
   public boolean test(CharSequence input){
      matcher.reset(input);
      return matcher.find();
   }
   public String getName(){return name;}

   Json appendNames(Json input){
      if(input == null){
         throw new IllegalArgumentException("input cannot be null");
      }
      Json ctx = input;
      for(Exp.NestPair pair : nesting){
         ctx.add(pair.getValue(),new Json());
         ctx = ctx.getJson(pair.getValue());
      }
      for(String value : fields.keySet()){
         ctx.set(value,fields.get(value).getType());
      }
      for(Exp child : children){
         child.appendNames(ctx);
      }
      return ctx;
   }
   public ValueType getType(String key){
      if(key == null){
         throw new IllegalArgumentException("key cannot be null");
      }
      return fields.get(key).getType();
   }
   public ValueMerge getMerge(String key){
      if(key == null){
         throw new IllegalArgumentException("key cannot be null");
      }
      return fields.get(key).getMerge();
   }

   public Exp requires(String key){
      if(key == null){
         throw new IllegalArgumentException("key cannot be null");
      }
      requires.add(key);
      return this;
   }
   public Exp enables(String key){
      if(key == null){
         throw new IllegalArgumentException("key cannot be null");
      }
      enables.add(key);
      return this;
   }
   public Exp disables(String key){
      if(key == null){
         throw new IllegalArgumentException("key cannot be null");
      }
      disables.add(key);
      return this;
   }

   public void onSetup(Parser parser){

   }
   public void onClose(Parser parser){
      if(hasRule(ExpRule.RemoveOnClose)){
         parser.remove(this);
      }else{
         if(hasChildren()){
            for(Exp child: children){
               child.onClose(parser);
            }
         }
      }
   }
   public Set<String> getRequires(){return requires;}
   public Set<String> getEnables(){return enables;}
   public Set<String> getDisables(){return disables;}
}
