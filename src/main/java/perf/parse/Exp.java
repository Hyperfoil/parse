package perf.parse;

import perf.parse.internal.CheatChars;
import perf.parse.internal.DropString;
import perf.parse.internal.IMatcher;
import perf.parse.internal.JsonBuilder;
import perf.parse.internal.RegexMatcher;
import perf.yaup.HashedLists;
import perf.yaup.StringUtil;
import perf.yaup.json.Json;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Matcher;

public class Exp {

   //final static XLogger logger = XLoggerFactory.getXLogger(MethodHandles.lookup().lookupClass());

   static final String NEST_ARRAY = "_array";
   static final String NEST_VALUE = "_value";

   public static final String CAPTURE_GROUP_PATTERN = "\\(\\?<([^>]+)>";
   public static final String ROOT_TARGET_NAME = "_ROOT";
   public static final String GROUPED_NAME = "_GROUPED";

   private static class ValueInfo {

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
   public enum NestType {Extend,Field,Name}
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
    * @param pattern
    * @return
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
    * @param pattern
    * @return
    */
   public static Map<String,ValueInfo> parsePattern(String pattern){
      Matcher fieldMatcher = java.util.regex.Pattern.compile(CAPTURE_GROUP_PATTERN).matcher(pattern);
      Map<String,ValueInfo> rtrn = new LinkedHashMap<>();

      while(fieldMatcher.find()){
         Queue<String> fieldKeys = new LinkedList<>(Arrays.asList(fieldMatcher.group(1).split(":")));
         String name = fieldKeys.poll();
         ValueInfo valueInfo = new ValueInfo(name,null,ValueType.Auto,ValueMerge.Auto);
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
               ValueType valueType = StringUtil.getEnum(type,ValueType.class);
               if(valueType!=null){
                  valueInfo.setType(valueType);
                  valueInfo.setTarget(value);
               }else{
                  //TODO log the error for invalid type?
               }
            }else{
               System.err.println("WTF is a "+key);
               throw new IllegalArgumentException("cannot infer type info from "+key+" in "+fieldMatcher.group(1)+" of "+pattern);
            }
         }
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

   public Exp(String pattern){
      this(pattern,pattern);
   }
   public Exp(String name, String pattern){
      this.name = name;
      this.pattern = pattern;
      this.fields = parsePattern(pattern);

      String safePattern = removePatternValues(pattern);
      this.matcher = new RegexMatcher(safePattern);
   }

   public String getPattern(){return pattern;}
   public int getEat(){return eat;}

   public Exp with(String key, Object value){
      with.putIfAbsent(key,value);
      return this;
   }
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
      List<String> nests = Json.dotChain(nesting);
      for(String nest : nests){
         int index=-1;
         if(nest.startsWith("${{") && nest.endsWith("}}")){
            String fieldName = nest.substring("${{".length(),nest.length()-"}}".length());
            key(fieldName);
         }else if (nest.startsWith(">")){
            String extendName = nest.substring(">".length());
            extend(extendName);
         }else{
            group(nest);
         }
      }
      return this;
   }
   public Exp group(String name){
      nesting.add(new NestPair(name,NestType.Name));
      return this;
   }
   public Exp key(String name){
      nesting.add(new NestPair(name,NestType.Field));
      return this;
   }
   public Exp extend(String name){
      nesting.add(new NestPair(name,NestType.Extend));
      return this;
   }

   public Exp eat(int width){
      eat = width;
      return this;
   }
   public Exp eat(Eat toEat){
      eat = toEat.getId();
      return this;
   }
   public Exp execute(MatchAction action){
      callbacks.add(action);
      return this;
   }
   public Exp add(Exp child){
      children.add(child);
      return this;
   }
   public boolean hasChildren(){return !children.isEmpty();}
   public void eachChild(Consumer<Exp> consumer){
      children.forEach(consumer);
   }
   public Exp setType(String fieldName, ValueType type){
      fields.get(fieldName).setType(type);
      return this;
   }
   public Exp setKeyValue(String key, String value){
      fields.get(key).setMerge(ValueMerge.Key);
      fields.get(key).setTarget(value);
      fields.get(value).setSkip(true);
      return this;
   }
   public Exp setMerge(String fieldName, ValueMerge merge){
      fields.get(fieldName).setMerge(merge);
      return this;
   }
   public Exp setRange(MatchRange range){
      this.matchRange = range;
      return this;
   }
   public Exp addRule(ExpRule rule){
      this.rules.put(rule,null);
      return this;
   }
   public Exp addRule(ExpRule rule, Object value){
      this.rules.put(rule,value);
      return this;
   }

   public Exp setMerge(ExpMerge merge){
      this.expMerge = merge;
      return this;
   }

   public boolean hasRule(ExpRule rule){
      return this.rules.containsKey(rule);
   }
   public Json getNestedTarget(Json currentTarget,JsonBuilder builder){
      Json returnTarget = currentTarget;
      if(nesting.isEmpty()){//for Tree
         switch (this.expMerge){
            case AsEntry:
               if(currentTarget == builder.getRoot()){
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
      JsonBuilder builder = new JsonBuilder();
      apply(new CheatChars(line),builder,null);
      return builder.getRoot();
   }
   public boolean apply(DropString line, JsonBuilder builder, Parser parser){
      return apply(line,builder,parser,line.reference(0));
   }
   protected boolean apply(DropString line, JsonBuilder builder, Parser parser, DropString.Ref startIndex){
      boolean rtrn = false;
      try {
         //cannot return for line.length==0 becausGe pattern may expect empty line
         if (startIndex.get() > line.length() && this.matchRange.equals(MatchRange.AfterParent)) {

            return false;
         }

         if (!this.requires.isEmpty() && parser != null) {
            boolean satisfyRequired = requires.stream()
               .filter(required -> !parser.getState(required))//return true if the requirement isn't satisfied
               .findAny()//will only find missing requirements
               .orElse(null) == null;

            if (!satisfyRequired) {
               return false;
            }
         }


         int matchStart = this.matchRange.apply(this.matcher, line, startIndex.get());


         if (this.matcher.find()) {
            rtrn = true;


            DropString.Ref firstStart = line.reference(matcher.start());
            DropString.Ref firstEnd = line.reference(matcher.end());


            //adding them to line causes line.drop to impact child matching
            //line.addReference(matcherStart);
            //line.addReference(matcherEnd);

            //run the pre-populate rules
            this.rules.forEach((rule, roleObjects) -> {
               rule.prePopulate(builder, roleObjects);
            });

            Json startTarget = builder.getTarget();
            Json currentTarget = startTarget;

            do {//repeat this


               DropString.Ref matcherStart = line.reference(matcher.start());
               DropString.Ref matcherEnd = line.reference(matcher.end());


               boolean needPop = false;
               boolean populateChangedTarget = false;
               currentTarget = getNestedTarget(startTarget, builder);
               if (currentTarget != startTarget) {//nesting pushes a temporary target
                  builder.pushTarget(currentTarget, getName() + GROUPED_NAME);
                  needPop = true;
               }
//               if(fields.values().stream().filter(v->v.getMerge().equals(ValueMerge.TargetId)).findAny().orElse(null) != null){
//                  System.out.println("PrePopulate: startTarget:"+startTarget.toString());
//                  System.out.println(builder.debug(true));
//               }

               populate(builder);
               if (currentTarget != builder.getTarget()) {//populating changed the target
                  currentTarget = builder.getTarget();
                  populateChangedTarget = true;
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
               Eat.preEat(this.eat, line, matcher.start(), matcher.end());

               Json ruleTarget = currentTarget;//ugh, lambdas
               this.rules.forEach((rule, roleObjects) -> {
                  rule.preChildren(builder, ruleTarget, roleObjects);
               });
               if (!disables.isEmpty() && parser != null) {
                  disables.forEach(v -> parser.setState(v, false));
               }
               if (!enables.isEmpty() && parser != null) {
                  enables.forEach(v -> parser.setState(v, true));
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
               }
               //default is to loop from end of current match
               //TODO how does this work if the Exp is EntireLine and doesn't eat?
               this.matchRange.apply(this.matcher, line, matcherEnd.get());


            } while (hasRule(ExpRule.Repeat) && this.matcher.find());
            if (rtrn) {//TODO also support pre-child match actions and call this postMatch?
               for (MatchAction action : callbacks) {
                  action.onMatch(line.getLine(), currentTarget, null /*TODO matchAction Exp*/, parser);
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
         System.out.println(getName()+" caught exception on line "+line.getLine());
         e.printStackTrace(System.out);
         System.exit(1);
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
            valueInfo.getMerge().merge(key,value,builder,target);
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
      return fields.get(key).getType();
   }
   public ValueMerge getMerge(String key){
      return fields.get(key).getMerge();
   }

   public Exp requires(String key){
      requires.add(key);
      return this;
   }
   public Exp enables(String key){
      enables.add(key);
      return this;
   }
   public Exp disables(String key){
      disables.add(key);
      return this;
   }
}
