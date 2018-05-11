package perf.parse;

import perf.parse.internal.CheatChars;
import perf.parse.internal.IMatcher;
import perf.parse.internal.JsonBuilder;
import perf.parse.internal.RegexMatcher;
import perf.yaup.HashedLists;
import perf.yaup.json.Json;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static perf.parse.Rule.*;

/**
 * Created by wreicher
 */
public class Exp {

    private static final String NEST_ARRAY = "_array";
    private static final String NEST_VALUE = "_value";

    private static final String CHAIN_SEPARATOR = ".";
    public static final String ROOT_TARGET_NAME = "_ROOT";
    public static final String GROUPED_NAME = "_GROUPED";

    private final Matcher fieldMatcher = java.util.regex.Pattern.compile("\\(\\?<([^>]+)>").matcher("");

    //Execute Rule
    private LinkedList<MatchAction> callbacks;

    private String pattern;
    private IMatcher matcher;
    private LinkedHashMap<String,String> fieldValues; //Map<Name,Value|name of value for KeyValue pair>

    private HashedLists<Rule,Object> rules;

    private LinkedHashSet<String> enables;
    private LinkedHashSet<String> disables;
    private LinkedHashSet<String> requires;

    private enum GroupType {Name,Extend,Key}

    private LinkedHashMap<String,GroupType> grouping;

    private int eat=Eat.Match.getId();

    private Merge merge = Merge.Collection;

    private String name;

    private LinkedList<Exp> children;

    private boolean debug=false;

    public Exp debug(){
        debug=true;
        return this;
    }
    public boolean isDebug(){return debug;}
    public String toString(){
        return getName()+" "+Eat.from(eat)+" "+merge;
    }

    public String matcherClass(){return matcher.getClass().toString();}


    public Exp(String name, String pattern){
        this.name = name;
        this.pattern = pattern;

        this.fieldValues = parsePattern(pattern);

        String safePattern = removePatternValues(pattern);

        this.matcher = new RegexMatcher(safePattern);

        this.callbacks = new LinkedList<>();

        this.rules = new HashedLists<>();

        this.grouping = new LinkedHashMap<>();

        this.children = new LinkedList<>();

        this.enables = new LinkedHashSet<>();
        this.disables = new LinkedHashSet<>();
        this.requires = new LinkedHashSet<>();


    }

    public String getPattern(){return this.pattern;}

    public Exp clone(){
        Exp rtrn = new Exp(this.getName(),this.getPattern());

        rtrn.eat(this.eat);
        rtrn.merge = this.merge;
        rtrn.fieldValues = copyMap(this.fieldValues);

        for(MatchAction action : callbacks){
            rtrn.execute(action);
        }
        for(Rule rule : rules.keys()){
            rtrn.rules.putAll(rule,rules.get(rule));
        }
        for(String group: this.grouping.keySet()){
            rtrn.grouping.put(group,this.grouping.get(group));
        }
        for(Exp child : children){
            rtrn.add(child.clone());
        }
        for(String enable : enables){
            rtrn.enables(enable);
        }
        for(String disable : disables){
            rtrn.disables(disable);
        }
        for(String require : requires){
            rtrn.requires(require);
        }
        return rtrn;
    }


    public Json appendNames(Json input){
        Json ctx = input;
        for(String name : grouping.keySet()){
            GroupType gt = grouping.get(name);

            ctx.add(name,new Json());
            ctx = ctx.getJson(name);
        }
        for(String value : fieldValues.keySet()){
            Value v = Value.from(fieldValues.get(value));
            ctx.set(value,v);
        }
        for(Exp child : children){
            child.appendNames(ctx);
        }
        return ctx;
    }
    public Json getNames(){
        Json rtrn = new Json();
        appendNames(rtrn);
        return rtrn;
    }
    public String getName(){return this.name;}

    public Exp forEachField(Consumer<String> fieldVisitor){
        for(String value : fieldValues.keySet()){
            fieldVisitor.accept(value);
        }
        return this;
    }

    public static long parseKMG(String kmg){
        Matcher m = java.util.regex.Pattern.compile("(?<number>\\d+\\.?\\d*)\\s?(?<kmg>[kmgtpezyKMGTPEZY]*)(?<bB>[bB]*)").matcher(kmg);
        if(m.matches()){

            double mult = 1;

            switch(m.group("kmg").toUpperCase()){
                case "Y": mult*=1024;//8
                case "Z": mult*=1024;//7
                case "E": mult*=1024;//6
                case "P": mult*=1024;//5
                case "T": mult*=1024;//4
                case "G": mult*=1024;//3
                case "M": mult*=1024;//2
                case "K": mult*=1024;//1
                case "B": mult*=1; // included for completeness
            }
            double bytes = m.group("bB").equals("b") ? 1.0/8 : 1;
            Double v =Double.parseDouble(m.group("number"))*mult*bytes;
            return v.longValue();
        }else{
            if(kmg.equals("-")){//trap for when dstat has a - value for a field (no idea why that happens but it does
              return 0;
            } else {
                throw new IllegalArgumentException(kmg + " does not match expected pattern for KMG");
            }
        }
    }

    /**
     * parses a java.util.Regex pattern to identify the group names and associated them with a default perf.parse.Value.List
     * @param pattern - the java.util.Regex pattern
     * @return - a LinkedHashMap<MatchGroupName,perf.parse.Value.List.getId()>
     */
    private LinkedHashMap<String,String> parsePattern(String pattern){
        LinkedHashMap<String,String> rtrn = new LinkedHashMap<>();

        fieldMatcher.reset(pattern);
        while(fieldMatcher.find()){
            Queue<String> fieldKeys = new LinkedList<>( Arrays.asList(fieldMatcher.group(1).split(":")) );
            String name = fieldKeys.poll();
            rtrn.put(name,Value.List.getId());
            //fieldKeys.remove(0); first poll should remove it
            while(!fieldKeys.isEmpty()){
                String key = "_"+fieldKeys.poll().toLowerCase()+"_";
                Value v = Value.from(key);


                if(v.equals(Value.Key)){
                    rtrn.put(name,key);
                }else{
                    rtrn.put(name,v.getId());
                }

            }
        }
        return rtrn;
    }
    private LinkedHashMap<String,String> copyMap(Map<String,String> map){
        LinkedHashMap<String,String> rtrn = new LinkedHashMap<>();
        for(String key : map.keySet()){
            rtrn.put(key,map.get(key));
        }
        return rtrn;
    }
    private String removePatternValues(String pattern) {
        String rtrn = pattern;
        fieldMatcher.reset(pattern);
        while(fieldMatcher.find()){
            String match = fieldMatcher.group(1);
            if(match.indexOf(":") > -1){
                rtrn = rtrn.replace(match,match.substring(0,match.indexOf(":")));
            }

        }
        return rtrn;
    }

    //Set status methods, all return a pointer to this for chaining

    public Exp group(String name){
        grouping.put(name,GroupType.Name);
        return this;
    }
    public Exp key(String name){
        grouping.put(name,GroupType.Key);
        return this;
    }
    //
    public Exp extend(String name){
        grouping.put(name,GroupType.Extend);
        return this;
    }

    /**
     * Set a custom width to to remove from the CheatChars input whenever the Exp matches
     * @param width the number of characters to remove
     * @return this
     */
    public Exp eat(int width){
        eat = width;
        return this;
    }
    /**
     * Defines how much of the input CheatChars should be removed if this Exp matches. The default is Eat.Match.
     * Eat.Line takes effect after all child iterations and self iterations while Eat.Match or custom width take effect
     * before child matching.
     * @param toEat
     * @return this
     */
    public Exp eat(Eat toEat){
        eat = toEat.getId();
        return this;
    }

    /**
     * Run the MatchAction AFTER all match iterations and child match iterations.
     * @param action
     * @return
     */
    public Exp execute(MatchAction action){
        callbacks.add(action);
        return this;
    }

    /**
     * Add the child Exp. child will run after each time the expression matches
     * @param child
     * @return
     */
    public Exp add(Exp child){
        children.add(child);
        return this;
    }
    public boolean hasChildren(){return !children.isEmpty();}
    public int childCount(){return children.size();}
    public Exp set(String name,Value value){
        if(value == Value.Key){
            throw new IllegalArgumentException("set(String name,Value value) cannot be used for Value.Key, use set(String name,String valueKey)");
        }
        fieldValues.put(name,value.getId());
        return this;
    }
    public Exp set(String name,String valueKey){
        if(fieldValues.containsKey(valueKey)){
            fieldValues.remove(valueKey);
        }
        fieldValues.put(name,valueKey);
        return this;
    }
    public Value get(String key){
        return Value.from(fieldValues.get(key));
    }
    /**
     * Add a Rule for action to take when the perf.parse.Exp matches the input
     * @param rule the rule to add
     * @return this
     */
    public Exp set(Rule rule){
        rules.put(rule,rule);
        return this;
    }

    public Exp set(Rule rule,Object object){
        rules.put(rule,object);
        return this;
    }

    /**
     * Set how the matching name value groups should be merged into the result of previous / subsequent Exp matches
     * @param merge
     * @return
     */
    public Exp set(Merge merge){
        this.merge = merge;
        return this;
    }

    //if the Exp will eat the given amount
    public boolean is(Eat e){
        return Eat.from(this.eat).equals(e);
    }

    //if the Exp has the rule
    public boolean is(Rule r){
        return rules.containsKey(r);
    }

    //the number of times Rule was set on this Exp
    public int count(Rule r){
        return rules.containsKey(r) ? rules.get(r).size() : 0;
    }

    // returns the role info form calls to #set(Rule,Object)
    public List<Object> getRuleInfo(Rule r){
        return rules.containsKey(r) ? rules.get(r).stream().filter(x->!(x instanceof Rule)).collect(Collectors.toList()) : Collections.emptyList();
    }
    public boolean is(Merge m){
        return this.merge == m;
    }
    public boolean is(String field,Value value){
        return value == Value.from(fieldValues.get(field));
    }
    private boolean hasValue(IMatcher m,Value v){
        boolean rtrn = false;
        for(String fieldName : fieldValues.keySet()){
            if(v.equals(Value.from(fieldValues.get(fieldName)))){
                rtrn = true;
            }
        }
        return rtrn;
    }
    private String fieldForValue(Value v,String defaultValue){
        String rtrn = defaultValue;
        for(String fieldName : fieldValues.keySet()){
            if(v.equals(Value.from(fieldValues.get(fieldName)))){
                rtrn = fieldName;
            }
        }
        return rtrn;
    }
    private List<String> chain(String keys){
        return new ArrayList<>(
            Arrays.asList(keys.split("\\.(?<!\\\\\\.)"))
        );
    }
    private String lastKey(String key){
        List<String> ids = chain(key);
        return ids.get(ids.size()-1).replaceAll("\\\\\\.",".");
    }
    private Json chain(Json json,String key){
        return chain(json,key,true);
    }
    private Json chain(Json json,String key,boolean dropLast) {
        Json rtrn = json;
        if(key.contains(CHAIN_SEPARATOR)){
            List<String> ids = chain(key);
            if(dropLast){
                ids.remove(ids.size()-1);
            }
            for(String id : ids){
                if(!rtrn.has(id)){
                    rtrn.set(id,new Json());
                }
                if( !(rtrn.get(id) instanceof Json) ){//this should never happen for our use case
                    Object existing = rtrn.get(id);
                    rtrn.set(id,new Json());
                    rtrn.getJson(id).add(existing);
                }
                rtrn = rtrn.getJson(id);
            }
        }
        return rtrn;
    }
    private Json chainGet(Json json,String key){
        Json rtrn = json;
        List<String> chain = chain(key);
        for(int i=0; i<chain.size()-1; i++){
            String current = chain.get(i);
            if(json.has(current)){
                rtrn = rtrn.getJson(current);
            }else{
                rtrn = null;
                break;
            }
        }
        return rtrn;
    }

    private boolean populate(IMatcher m,JsonBuilder builder){

        if(isDebug()){
            System.out.println(getName()+" > populate ");
            System.out.println("    "+builder.debug(true).replaceAll("\n","\n    "));
        }


        boolean changedTarget = false;

        for(String fieldName : fieldValues.keySet()){
            String vString = fieldValues.get(fieldName);
            Value v = Value.from(vString);
            String fieldValue = m.group(fieldName);

            switch(v){
                case TargetId: {
                    Object value = fieldValue;
                    if (Pattern.matches("\\d+", fieldValue)) {//long
                        value = Long.parseLong(fieldValue);
                    } else if (Pattern.matches("\\d+\\.\\d+", fieldValue)) {//double
                        value = Double.parseDouble(fieldValue);
                    } else {
                    }

                    if (!builder.getTarget().has(fieldName)) {//add event info, assume it belongs to the same event
                        builder.getTarget().set(fieldName, value);

                    } else if (value.equals(builder.getTarget().get(fieldName))) {//same event

                    } else {
                        builder.close();
                        builder.getTarget().set(fieldName, value);
                        changedTarget = true;
                    }
                    break;
                }
                case NestPeerless:
                case NestLength:

                    int length = fieldValue.length();
                    if( builder.hasContext(fieldName,true) ) { // the current context is already part of the tree

                        int contextLength = builder.getContextInteger(fieldName,true);
                        if(isDebug()){
                            System.out.println(getName()+"  has "+fieldName+" above in "+builder.getTarget());
                            System.out.println("  length="+length+" context."+fieldName+"="+builder.getContextInteger(fieldName,true));
                        }

                        if( length > builder.getContextInteger(fieldName,true) ){//the current match needs to be a child of target

                            if(isDebug()){
                                System.out.println("  CHILD");
                            }
                            Json childAry = new Json();
                            Json newChild = new Json(false);

                            childAry.add(newChild);
                            builder.getTarget().add(fieldName,childAry);

                            builder.pushTarget(childAry);
                            builder.setContext(fieldName+NEST_ARRAY,true);
                            builder.pushTarget(newChild);

                            changedTarget = true;

                        } else if( length < builder.getContextInteger(fieldName,true) ) { // elder (ancestor but not parent)

                            if(isDebug()){
                                System.out.println("  ELDER");
                                System.out.println("    "+builder.debug(true).replaceAll("\n","\n    "));
                            }

                            while(length < builder.getContextInteger(fieldName,true) ||
                                    builder.getContextBoolean(fieldName+NEST_ARRAY,false)) {
                                if(isDebug()){
                                    System.out.println("    needPop: "+length+" < "+
                                        builder.getContextInteger(fieldName,true) +
                                        "|| NEST_ARRAY ? "+
                                        builder.getContextBoolean(fieldName+NEST_ARRAY,false)+
                                        " builder.size="+builder.size()
                                    );
                                    System.out.println("      "+builder.debug(true).replaceAll("\n","\n      "));
                                }
                                builder.popTarget();
                                if(isDebug()){
                                    System.out.println("    postPop:");
                                    System.out.println("      "+builder.debug(false));
                                }
                                changedTarget = true;
                            }
                            //at this point should it be pointing at the sibling


                            //System.out.println(m.group("category"));
                            //
                            if(isDebug()){
                                System.out.println("    ElderTarget");
                                System.out.println("      "+builder.debug(false).replaceAll("\n","\n    "));
                            }

                            if(isDebug()){
                                System.out.println("    addSibling");
                                System.out.println("    one more pop");
                            }

                            //aimed at the previous entry
                            if(Value.NestPeerless.equals(v)){
                                if( isDebug() ){
                                    System.out.println("    NestPeerless, us last entry in nest_array");
                                }
                                Json lastEntry = builder.getTarget().getJson(builder.getTarget().size()-1);
                                if( isDebug() ){
                                    System.out.println("      lastEntry = "+lastEntry);
                                    System.out.println("      context\n"+builder.debug(false));
                                }

                            }else{

                                //isn't pointed at array for PrintGcFactoryTest.newParser_g1gc_details_nest
                                //TODO pops root for PrintGcFactoryTest.newParser_g1gc_details_nest
                                builder.popTarget();//points at array??

                                if(isDebug()){
                                    System.out.println("    NestLength, add entry to elder");
                                    System.out.println("    target ="+builder.getTarget());
                                    System.out.println("    context = "+builder.debug(false));
                                }
                                Json newEntry = new Json(false);
                                //builder.getTarget().add(fieldName,newEntry);

                                //if pointed at the nest-array
                                if(builder.getContextBoolean(fieldName+NEST_ARRAY,false)){
                                    builder.getTarget().add(newEntry);//change to add to array to avoid having to pop the NEST_ARRAY
                                }else{
                                    //likely an indent less than the initial indent that started the array, tree as new tree
                                    builder.getTarget().add(fieldName,newEntry);

                                    builder.pushTarget(builder.getTarget().getJson(fieldName));
                                    builder.setContext(fieldName+NEST_ARRAY,true);
                                    builder.pushTarget(newEntry);

                                }

                                builder.pushTarget(newEntry);
                            }
                            //pop the previous entry (sibling of this new entry)
                            changedTarget = true;
                            //target = newEntry;
                            //need to add a new entry for the context to use?
                        } else { // sibling
                            if(isDebug()){
                                System.out.println("  SIBLING");
                                //need to look recursively in case child Exp pushed targets
                                if(builder.hasContext(fieldName+NEST_VALUE,true)){
                                    System.out.println("    context.nest_value=||"+builder.getContextString(fieldName+NEST_VALUE,true)+"||");
                                    System.out.println("            nest_value=||"+fieldValue+"||");
                                }
                            }
                            if(Value.NestPeerless.equals(v)){
                                if(isDebug()){
                                    System.out.println("    NestPeerless");
                                }
                            }else{//NestLength

                                if(isDebug()){
                                    System.out.println("    NestLength");
                                }
                                //current context is somewhere above the NEST_ARRY, need to pop until we are aimed at the array?
                                while (builder.size()>1 && !builder.getContextBoolean(fieldName + NEST_ARRAY, false)) {
                                    if (isDebug()) {
                                        System.out.println("    NEST_ARRAY ? " + builder.getContextBoolean(fieldName + NEST_ARRAY, false));
                                    }
                                    builder.popTarget();//
                                    if (isDebug()) {
                                        System.out.println("      target =" + builder.getTarget());
                                        System.out.println("      context = " + builder.debug(false));
                                    }
                                }
                                //don't need to create the array because it muse exist for this to be a peer
                                Json newJSON = new Json(false);

                                builder.getTarget().add(newJSON);//adding to to existing fieldName NEST_ARRAY
                                builder.pushTarget(newJSON);
                                //target = newJSON;
                                changedTarget = true;

                            }
                        }
                    }else{//start the tree

                        if(isDebug()){
                            System.out.println("  creating nest "+fieldName+" on target: "+builder.getTarget());
                        }

                        Json treeArry = new Json(true);
                        Json treeStart = new Json(false);

                        treeArry.add(treeStart);

                        builder.getTarget().add(fieldName,treeArry);

                        builder.pushTarget(treeArry);
                        builder.setContext(fieldName+NEST_ARRAY,true);
                        builder.pushTarget(treeStart);

                        changedTarget = true;
                    }

                    builder.setContext(fieldName,length);

                    if(changedTarget && !builder.hasContext(fieldName+NEST_VALUE,false)){
                        if(isDebug()){
                            System.out.println("    set NEST_VALUE=||"+fieldValue+"||");
                        }
                        builder.setContext(fieldName+NEST_VALUE,fieldValue);
                    }
                    if(isDebug()){
                        System.out.println("  POST-NEST:");
                        System.out.println("      "+builder.debug(true).replaceAll("\n","\n      "));
                    }

                    break;
                case KMG:
                    long kmg = parseKMG(fieldValue);
                    //String kmg = fieldValue;
                    chain(builder.getTarget(),fieldName).set(lastKey(fieldName),kmg);
                    break;
                case Count:
                    chain(builder.getTarget(),fieldName,false).set(fieldValue,1+chain(builder.getTarget(),fieldName,false).getLong(fieldValue,0));
                    break;
                case Sum:
                    double sum = Double.parseDouble(fieldValue);
                    chain(builder.getTarget(),fieldName).set(lastKey(fieldName),chain(builder.getTarget(),fieldName).getDouble(lastKey(fieldName),0)+sum);
                    break;
                case Key: {
                    String keyValue = m.group(vString);
                    Object toSet = keyValue;
                    if (Pattern.matches("\\d+", keyValue)) {//long
                        toSet = Long.parseLong(keyValue);
                    } else if (Pattern.matches("\\d+\\.\\d+", keyValue)) {//double
                        toSet = Double.parseDouble(keyValue);
                    } else {
                    }
                    if (!keyValue.isEmpty()) {
                        chain(builder.getTarget(), fieldName).set(fieldValue, toSet);
                    }
                    break;
                }
                case BooleanKey:
                    chain(builder.getTarget(),fieldName).set(lastKey(fieldName),true);
                    break;
                case BooleanValue:
                    chain(builder.getTarget(),fieldName).set(fieldValue,true);
                    break;
                case Position:
                    chain(builder.getTarget(),fieldName).set(lastKey(fieldName),m.start());
                    break;
                case String:
                    chain(builder.getTarget(),fieldName).set(lastKey(fieldName),chain(builder.getTarget(),fieldName).getString(lastKey(fieldName),"")+fieldValue);
                    break;
                case First: {
                    Object firstValue = fieldValue;
                    if (Pattern.matches("\\d+", fieldValue)) {//long
                        firstValue = Long.parseLong(fieldValue);
                    } else if (Pattern.matches("\\d+\\.\\d+", fieldValue)) {//double
                        firstValue = Double.parseDouble(fieldValue);
                    } else {
                    }
                    Json chained = chain(builder.getTarget(), fieldName);
                    if (!chained.has(lastKey(fieldName))) {
                        chained.set(lastKey(fieldName), firstValue);
                    }
                    break;
                }
                case Last:
                    Object lastValue = fieldValue;
                    if(Pattern.matches("\\d+",fieldValue)){//long
                        lastValue = Long.parseLong(fieldValue);
                    }else if (Pattern.matches("\\d+\\.\\d+",fieldValue)){//double
                        lastValue = Double.parseDouble(fieldValue);
                    }else{}
                    chain(builder.getTarget(),fieldName).set(lastKey(fieldName),lastValue);
                    break;
                case Set:

                    Object value = fieldValue;
                    if(Pattern.matches("\\d+",fieldValue)){//long
                        value = Long.parseLong(fieldValue);
                    }else if (Pattern.matches("\\d+\\.\\d+",fieldValue)){//double
                        value = Double.parseDouble(fieldValue);
                    }else{}
                    Json chained = chain(builder.getTarget(),fieldName);
                    String lastKey = lastKey(fieldName);

                    if(chained.has(lastKey)){
                        if(! (chained.get(lastKey) instanceof Json)){
                            Object existing = chained.get(lastKey);
                            if(!existing.equals(value)){
                                Json set = new Json();
                                set.add(existing);
                                set.add(value);
                                chained.set(lastKey,set);
                            }
                        }else{
                            Json existingSet = chained.getJson(lastKey);
                            //check if existingSet already contains value
                            final Object refValue = value;
                            List<Object> found = existingSet.values().stream().filter((k)-> refValue.equals(k)).collect(Collectors.toList());
                            if(found.isEmpty()){
                                existingSet.add(value);
                            }
                        }
                    }else{
                        Json set = new Json();
                        set.add(value);
                        chained.set(lastKey,set);
                    }

                    break;
                case List:
                default:
                    Object newObj = fieldValue;
                    if(Pattern.matches("\\d+",fieldValue)){//long
                        newObj = Long.parseLong(fieldValue);
                    }else if (Pattern.matches("\\d+\\.\\d+",fieldValue)){//double
                        newObj = Double.parseDouble(fieldValue);
                    }else{}
                    chain(builder.getTarget(),fieldName).add(lastKey(fieldName),newObj);
                    break;
            }
        }
        if(isDebug()){
            System.out.println(getName()+" < populate");
            System.out.println("  target  "+builder.getTarget());
            System.out.println("  rtrn    "+changedTarget);
        }

        return changedTarget;
    }
    public boolean test(CharSequence input){
        matcher.reset(input);
        return matcher.find();
    }
    public Json apply(String line){
        JsonBuilder builder = new JsonBuilder();
        apply(new CheatChars(line),builder,null);
        return builder.getRoot();
    }
    public boolean apply(CheatChars line, JsonBuilder builder, Parser parser){
        boolean result = applyWithStart(line,builder,parser,new AtomicInteger(0));
        return result;
    }

    public Exp enables(String state){
        this.enables.add(state);
        return this;
    }
    public Exp disables(String state){
        this.disables.add(state);
        return this;
    }
    public Exp requires(String state){
        this.requires.add(state);
        return this;
    }

    //changed to atomic integer so pattern can change start offset if it eats before start
    private boolean applyWithStart(CheatChars line, JsonBuilder builder, Parser parser, AtomicInteger start){
        if(isDebug()){
            System.out.println(this.getName()+" > applyWithStart @ "+start+": line=||"+line+"||");
            System.out.println("  line@start=||"+line.subSequence(start.get(),line.length())+"||");
        }

        if(!this.requires.isEmpty() && parser!=null){
            boolean satisfyRequired = true;
            for(String required : requires){
                boolean hasState = parser.getState(required);
                satisfyRequired = satisfyRequired & hasState;
                if(isDebug() && !hasState ){
                    System.out.println("  "+getName()+" missing "+required);
                }
            }
            if( !satisfyRequired ){
                return false;
            }
        }

        boolean rtrn = false;
        matcher.reset(line);
        int startPoint = is(Rule.LineStart) ? 0 : start.get();
        if(isDebug() && startPoint != start.get()){
            System.out.println("  startPoint="+startPoint);
        }
        matcher.region( startPoint,line.length() );

        if(matcher.find()){

            //System.out.println("  "+getName()+" "+line.subSequence(matcher.start(),matcher.end())+" ||"+line.subSequence(startPoint,line.length())+"||");

            if(isDebug()){
                System.out.println("  MATCHED ||"+line.subSequence(matcher.start(),matcher.end())+"||");
                System.out.println("      "+builder.debug(true).replaceAll("\n","\n      "));
            }

            rtrn = true;
            if ( is(Merge.PreClose) ) {

                if(isDebug()){
                    System.out.println("    PreClose");
                }

                builder.close();
            }else if( is(Rule.PrePopTarget) ) {
                List<Object> ruleInfo = getRuleInfo(PrePopTarget);
                if(ruleInfo.isEmpty()) {
                    if (isDebug()) {
                        System.out.println("    PrePopTarget");
                    }

                    builder.popTarget();
                }else{
                    ruleInfo.forEach(object-> builder.popTarget(object.toString()));
                }
            }else if ( is(Rule.PreClearTarget)){
                List<Object> ruleInfo = getRuleInfo(PreClearTarget);
                if(ruleInfo.isEmpty()){
                    if(isDebug()) {
                        System.out.println("    PreClearTarget");
                    }
                    builder.clearTargets();
                }else{
                    if(isDebug()){
                        System.out.println("PreClearTarget(s)::"+ruleInfo);
                    }
                    ruleInfo.forEach(object-> builder.clearTargets(object.toString()));
                }

            }

            if(is(TargetRoot)){
                builder.pushTarget(builder.getRoot(),getName()+ROOT_TARGET_NAME);
            }

            Json startTarget = builder.getTarget();

            Json target = startTarget;
            boolean needPop = false;

            do {
                //TODO probably broken if a rule is grouped and repeated? worry it will repeatedly push targets

                target = startTarget;
                Json grouped = target;
                if(grouping.isEmpty()){
                    if( is(Merge.Entry) ){
                        if( isDebug() ){
                            System.out.println("  > Entry w/o grouping");
                            System.out.println("      "+builder.debug(true).replaceAll("\n","\n      "));
                        }
                        if(target == builder.getRoot()){
                            if(isDebug()){
                                System.out.println("    target==ROOT");
                            }
                        }else if(target.isEmpty()){
                            if( isDebug() ){
                                System.out.println("    target is empty, creating a new entry");
                            }
                            Json newEntry = new Json();
                            target.add(newEntry);
                            builder.pushTarget(newEntry);
                            target = builder.getTarget();
                            grouped = target;
                            //needPop = true;

                        }else if (!target.isArray()){

                            if( isDebug() ){
                                System.out.println("    target is not an array, find an array");
                                System.out.println("      target "+target);
                                System.out.println("      peekT  "+builder.peekTarget(1));
                                System.out.println("      "+builder.debug(true).replaceAll("\n","\n      "));

                            }

                            if(builder.peekTarget(1) !=null && builder.peekTarget(1).isArray()){

                                if( isDebug() ){
                                    System.out.println("    parent is an array, create a new entry there");
                                }
                                Json newTarget = new Json();
                                builder.popTarget();
                                builder.getTarget().add(newTarget);
                                builder.pushTarget(newTarget);

                                target = builder.getTarget();
                                grouped = target;

                            }


                        }else{
                            if( isDebug() ){
                                System.out.println("    target is an array (non-root) and we are an entry");
                            }
                        }

                        if( isDebug() ){
                            System.out.println("  < Entry w/o grouping");
                            System.out.println("      "+builder.debug(true).replaceAll("\n","\n      "));

                        }
                    }
                }
                for(Iterator<String> groupIter = grouping.keySet().iterator(); groupIter.hasNext();){
                    //TODO handle case where groupName should be an integer key?
                    String groupName = groupIter.next();
                    GroupType groupType = grouping.get(groupName);
                    boolean extend = false;
                    if(isDebug()){
                        System.out.println("  grouping "+groupType+"="+groupName);
                    }
                    if( GroupType.Key.equals(groupType) ){
                        groupName = matcher.group( groupName );
                        if( groupName.isEmpty() ) {
                            throw new IllegalArgumentException("Cannot group with "+groupName+", match not found in line="+line);
                        }
                    }
                    if( GroupType.Extend.equals(groupType) ) {
                        extend=true;
                    }
                    if ( !groupIter.hasNext() ) {
                        if ( is(Merge.Entry) ) {
                            if( grouped.has(groupName) ) {
                                if(isDebug()){
                                    System.out.println("    already has "+groupName);
                                }
                                Json entry = new Json(false);
                                grouped.add(groupName,entry);
                                grouped = entry;
                            } else {
                                if(isDebug()){
                                    System.out.println("    does not have "+groupName);
                                }
                                Json entry = new Json(false);
                                Json arry = new Json();
                                arry.add(entry);
                                grouped.set(groupName,arry);
                                grouped = entry;
                            }
                        } else if ( is(Merge.Extend) ) {
                            if( grouped.has(groupName) ) {
                                Json arry = grouped.getJson(groupName);
                                Json last = arry.getJson(arry.size()-1);
                                grouped = last;
                            }else{
                                Json entry = new Json(false);
                                Json arry = new Json();
                                arry.add(entry);
                                grouped.set(groupName,arry);
                                grouped = entry;
                            }

                        } else if ( is(Merge.Collection) ) {
                            if( grouped.has(groupName) ) {
                                if( !(grouped.get(groupName) instanceof Json) ){
                                    System.out.println("groupName "+groupName+" not a group");
                                }
                                grouped = grouped.getJson(groupName);
                            } else {
                                Json newJSON = new Json(false);
                                grouped.set(groupName,newJSON);
                                grouped = newJSON;

                                //add to fix nestlength ?
                                //builder.pushTarget(newJSON);
                            }
                        } else {
                            //will happen if NewEntry, no action because new-entry merges with the current context :)
                            if(isDebug()){
                                System.out.println("    new entry automatically merges with existing context");
                            }
                        }
                    } else {
                        //same behavior as Merge.Collection
                        if(grouped.has(groupName)){
                            if(isDebug()){
                                System.out.println("    already has "+groupName);
                            }
                            //could be an array
                            Object obj = grouped.get(groupName);
                            if(obj instanceof Json && ((Json)obj).isArray()){
                                Json groupArry = (Json)obj;
                                if( (extend || is(Merge.Extend)) && groupArry.size()>0){
                                    grouped = groupArry.getJson(groupArry.size()-1);
                                }else{
                                    Json newInstance = new Json(false);
                                    groupArry.add(newInstance);
                                    grouped = newInstance;
                                }
                            }else {
                                grouped = grouped.getJson(groupName);
                            }
                        }else{
                            Json newJSON = new Json(false);
                            grouped.set(groupName,newJSON);
                            grouped = newJSON;

                        }
                    }
                }


                if(target != grouped){
                    target = grouped; // will only change target if there was a grouping
                    builder.pushTarget(target,getName()+GROUPED_NAME);
                    needPop = true; //removed because it breaks child.PushTarget

                }

                //builder.setTargetRoot(is(Rule.TargetRoot));
                //TODO need to push root earlier
                boolean changedContext = populate(matcher,builder);
                if(is(TargetRoot)){
                    builder.popTarget(getName()+ROOT_TARGET_NAME);
                }

                //builder.setTargetRoot(false);

                if( changedContext ){//NestLength or TargetId
                    Json cc = builder.getTarget();
                    target = cc;
                }

                Eat toEat = Eat.from(this.eat);


                int mStart = matcher.start();
                int mEnd = matcher.end();
                int currentStart = start.get();

                CheatChars childLine = line;
                AtomicInteger childStart = new AtomicInteger(mEnd);

                if(is(ChildrenLookBehind)){
                    childLine = line.subSequence(0,mStart);
                    childStart.set(0);
                }
                switch(toEat){
                    case ToMatch:

                        line.drop(0,mEnd);
                        if(start.get() > mEnd){
                            if(isDebug()){
                                System.out.println(getName()+"    set start = "+(start.get()-mEnd));
                            }
                            start.set(start.get()-mEnd);
                        }else{
                            if(isDebug()){
                                System.out.println(getName()+"    set start = 0");
                            }

                            start.set(0);
                        }
                        mStart = 0;
                        matcher.region(mStart,line.length());
                        mEnd = 0;
                        break;
                    case Line://Do nothing until after all children matches
                    case Match:
                        int mStop = matcher.end();
                        line.drop(mStart,mStop);
                        //start inside match
                        if(isDebug()){
                            System.out.println(" "+getName()+" match currentStart="+currentStart+" mStop="+mStop+" mStart="+mStart);

                        }
                        if(currentStart <= mStop && currentStart > mStart){
                            if(isDebug()){
                                System.out.println(getName()+"    set start = "+mStart);
                            }
                            start.set(mStart);
                        }else if (currentStart > mStop) {//start after match
                            if(isDebug()){
                                System.out.println(getName()+"    set start = "+(currentStart-(mStop-mStart)));
                            }
                            start.set(currentStart-(mStop-mStart));
                        }else{
                            //if start was before the match
                        }
                        matcher.region(mStart,line.length());
                        mEnd = mStart;
                        break;
                    case Width:
                        int wStop = this.eat;
                        //drop(mStart,wStop or mStart+wStop)?
                        line.drop(mStart,mStart+wStop);
                        //start inside match
                        if(currentStart <= mStart+wStop && currentStart >= mStart){
                            if(isDebug()){
                                System.out.println(getName()+"    set start = "+(mStart));
                            }

                            start.set(mStart);
                        }else if (currentStart > mStart+wStop){
                            if(isDebug()){
                                System.out.println(getName()+"    set start = "+(currentStart- (wStop)));
                            }

                            start.set(currentStart- (wStop));
                        }
                        matcher.region(mStart,line.length());
                        mEnd = mStart;
                        break;
                }

                if(isDebug() && currentStart != start.get()){
                    System.out.println(this.getName()+" changed start from "+currentStart+" to "+start.get());
                    System.out.println("  line@oldStart=||"+line.subSequence(currentStart,line.length())+"||");
                    System.out.println("  line@newStart=||"+line.subSequence(start.get(),line.length())+"||");
                }

                //moved before children to preserve target order
                if( is(Rule.PushTarget) ) {
                    List<Object> ruleInfo = getRuleInfo(PushTarget);
                    if(ruleInfo.isEmpty()) {
                        if (isDebug()) {
                            System.out.println("Rule.PushTarget=" + target);
                        }
                        builder.pushTarget(target);
                        if (isDebug()) {
                            System.out.println("Builder:\n" + builder.debug(true));
                        }
                    }else{
                        final Json ruleTarget = target;//ugh, lambdas
                        ruleInfo.forEach(object->builder.pushTarget(ruleTarget,object.toString()));
                    }
                }



                //moved enable disable to before child
                //update the parser states after looping
                if(!disables.isEmpty() && parser!=null){
                    if(isDebug()){System.out.println(this.getName()+" disables "+disables);}
                    disables.forEach((disable)->parser.setState(disable, false));
                }
                if(!enables.isEmpty() && parser!=null){
                    if(isDebug()){System.out.println(this.getName()+" enables "+enables);}
                    enables.forEach(((enable)->parser.setState(enable,true)));
                }
                //call each child
                int lineLength = line.length();
                boolean childMatched = false;
                //TODO line needs to decrease in size with each match or we infinite loop
                //TODO BUG if child eats then mEnd may be wrong (if child is LineStart
                do {
                    if(!is(ChildrenLookBehind)){
                        childStart.set(mEnd);
                    }

                    childMatched=false;
                    for (Exp child : children) {
                        int childStartBefore = childStart.get();
                        boolean thisChildMatched = child.applyWithStart(childLine, builder, parser, childStart);
                        if(thisChildMatched){
                            if (isDebug() && !builder.getTarget().equals(target) ){
                                System.out.println("child changed target");
                            }
                        }
                        if(thisChildMatched && childStartBefore != childStart.get()){
                            if(isDebug()){
                                System.out.println(child.getName()+" changed childStart, need to update "+this.getName());
                            }
                            //we probably need to update start
                            currentStart = start.get();
                            if( currentStart > childStart.get() && currentStart < childStartBefore){
                                start.set( childStart.get());
                            }else if( currentStart > childStart.get() ){
                                start.set( currentStart - (childStart.get() - childStartBefore));
                            }else{

                            }
                        }
                        childMatched = thisChildMatched || childMatched;
                    }
                }while(childMatched && is(Rule.RepeatChildren));

                //if the children modified the line the matcher needs to reset
                if(lineLength != line.length()){
                    matcher.reset(line);
                }
                //trying to pop the auto target for group / nest / key is breaking jdk9 heap parsing6td
                if( needPop && !changedContext) {
//                    System.out.println(getName()+" popTarget "+popIndex+" size="+builder.depth());
//                    System.out.println(builder.debug(true));
                    Json poped = builder.popTarget(getName()+GROUPED_NAME);//pop the group target for this Exp (preserves child targets)
//                    System.out.println("  postPop: "+getName()+GROUPED_NAME);
//                    System.out.println("  "+builder.debug(true).replaceAll("\n","\n  "));
                    //System.out.println(poped.toString(0));
                }
                //only notify the callbacks for the last occurrence of a match
                if(!is(Rule.Repeat)) {
                    for (MatchAction action : callbacks) {
                        action.onMatch(line.toString(), target, this, parser);
                    }
                }

            }while( is(Rule.Repeat) && matcher.find() );



            //only notify the callbacks for the last occurrence of a match
            if( is(Rule.Repeat) && rtrn){
                for (MatchAction action : callbacks) {
                    action.onMatch(line.getOriginalLine(), target, this, parser);
                }
            }

            if ( is(Merge.PostClose) ) {
                if (isDebug()) {
                    System.out.println("    PostClose");
                }
                builder.close();
            }
            if( is(Rule.PostPopTarget) ) {
                if(isDebug()){
                    System.out.println(">> Rule.PostPopTarget");
                    System.out.println(builder.debug(true));

                }
                List<Object> ruleInfo = getRuleInfo(PostPopTarget);
                if(ruleInfo.isEmpty()){
                    int count = count(Rule.PostPopTarget);
                    if(isDebug()){
                        System.out.println("   count = "+count);
                    }
                    builder.popTarget(count);
                    if(isDebug()){
                        System.out.println("<< Rule.PostPopTarget");
                        System.out.println(builder.debug(true));
                    }
                }else{//pop specific targets
                    ruleInfo.forEach(object->{
                        builder.popTarget(object.toString());
                    });
                }
            }
            if( is(Rule.PostClearTarget) ) {
                List<Object> ruleInfo = getRuleInfo(Rule.PostClearTarget);
                if(ruleInfo.isEmpty()) {
                    builder.clearTargets();
                }else{
                    //System.out.println(getName()+" PostClearing "+ruleInfo);
                    ruleInfo.forEach(object-> builder.clearTargets(object.toString()));
//                    System.out.println("PostCleared");
//                    System.out.println(builder.debug(true));
                }
            }

            if(Eat.from(this.eat) == Eat.Line){ // eat the line after applying children and repeating
                line.drop(0,line.length());
            }

        }//matched
        else {
            if(isDebug()){
                System.out.println("  NOT matched");
            }
        }

        return rtrn;
    }
    public static String pad(int i){
        if(i<=0)
            return "";
        return "                                                                                                                                                                                                              ".substring(0,i);
    }
}
