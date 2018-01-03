package perf.parse;

import org.json.JSONArray;
import org.json.JSONObject;
import perf.parse.internal.CheatChars;
import perf.parse.internal.IMatcher;
import perf.parse.internal.JsonBuilder;
import perf.parse.internal.RegexMatcher;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.function.Consumer;
import java.util.regex.Matcher;

/**
 * Created by wreicher
 */
public class Exp {

    public static final String CHILD_KEY = "_children";

    private final Matcher fieldMatcher = java.util.regex.Pattern.compile("\\(\\?<([^>]+)>").matcher("");

    //Execute Rule
    private LinkedList<MatchAction> callbacks;

    private IMatcher matcher;
    private LinkedHashMap<String,String> fieldValues; //Map<Name,Value|name of value for KeyValue pair>

    private LinkedHashSet<Rule> rules;
    private LinkedHashSet<String> enables;
    private LinkedHashSet<String> disables;
    private LinkedHashSet<String> requires;

    private enum GroupType {Name,Extend,Key};

    private LinkedHashMap<String,GroupType> grouping;

    private int eat=Eat.None.getId();

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

        this.matcher = /* StringMatcher.canMatch(pattern) ? new StringMatcher(pattern) :*/ new RegexMatcher(pattern);

        this.fieldValues = parsePattern(pattern);

        this.callbacks = new LinkedList<>();

        this.rules = new LinkedHashSet<>();

        this.grouping = new LinkedHashMap<>();

        this.children = new LinkedList<>();

        this.enables = new LinkedHashSet<>();
        this.disables = new LinkedHashSet<>();
        this.requires = new LinkedHashSet<>();

    }


    public JSONObject appendNames(JSONObject input){
        JSONObject ctx = input;
        for(String name : grouping.keySet()){
            GroupType gt = grouping.get(name);

            ctx.accumulate(name,new JSONObject());
            ctx = ctx.getJSONObject(name);
        }
        for(String value : fieldValues.keySet()){
            Value v = Value.from(fieldValues.get(value));
            ctx.put(value,v);
        }
        for(Exp child : children){
            child.appendNames(ctx);
        }
        return ctx;
    }
    public JSONObject getNames(){
        JSONObject rtrn = new JSONObject();
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
        Matcher m = java.util.regex.Pattern.compile("(?<number>\\d+\\.?\\d*)(?<kmg>[kmgtpezyKMGTPEZY]*)(?<bB>[bB]*)").matcher(kmg);
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
            rtrn.put(fieldMatcher.group(1),Value.List.getId());
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
    public Exp extend(String name){
        grouping.put(name,GroupType.Extend);
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

    /**
     * Add a Rule for action to take when the perf.parse.Exp matches the input
     * @param rule the rule to add
     * @return this
     */
    public Exp set(Rule rule){
        rules.add(rule);
        return this;
    }

    /**
     * Set how the matching name value groups should be merged into the result of previous / subsequent perf.parse.Exp matches
     * @param merge
     * @return
     */
    public Exp set(Merge merge){
        this.merge = merge;
        return this;
    }

    //Check status methods
    public boolean is(Rule r){
        return rules.contains(r);
    }
    public boolean is(Merge m){
        return this.merge == m;
    }
    public boolean is(String field,Value value){
        return value == Value.from(fieldValues.get(field));
    }

    private boolean populate(IMatcher m,JsonBuilder builder){
        boolean changedContext = false;
        JSONObject targetContext = builder.getCurrentContext();

        for(String fieldName : fieldValues.keySet()){
            String vString = fieldValues.get(fieldName);
            Value v = Value.from(vString);
            String fieldValue = m.group(fieldName);

            switch(v){
                case NestLength:
                    int length = fieldValue.length();
                    if( targetContext.has(fieldName) ) { // child

                        if( length > targetContext.getInt(fieldName) ){

                            JSONObject newChild = new JSONObject();
                            targetContext.append(CHILD_KEY,newChild);
                            builder.setCurrentContext(newChild);

                            targetContext = newChild;
                        } else if( length < targetContext.getInt(fieldName) ) { // parent

                            builder.popContext();
                            targetContext = builder.getCurrentContext();

                        } else { // sibling
                            builder.popContext();

                            targetContext = builder.getCurrentContext();
                            JSONObject newJSON = new JSONObject();
                            targetContext.append(CHILD_KEY, newJSON);
                            builder.setCurrentContext(newJSON);
                            targetContext = newJSON;
                        }
                        changedContext = true;
                    }

                    //NestLength starts a new Object
                    targetContext.put(fieldName,length);

                    break;
                case Number:
                    double number = Double.parseDouble(fieldValue);
                    targetContext.put(fieldName,number);
                    break;
                case KMG:
                    long kmg = parseKMG(fieldValue);
                    targetContext.put(fieldName,kmg);
                    break;
                case Count:
                    targetContext.increment(fieldValue);
                    break;
                case Sum:
                    double sum = Double.parseDouble(fieldValue);
                    targetContext.put(fieldName,targetContext.optDouble(fieldName,0)+sum);
                    break;
                case Key:
                    String keyValue = m.group(vString);
                    if(!keyValue.isEmpty()){
                        targetContext.put(fieldValue,keyValue);
                    }
                    break;
                case BooleanKey:
                    targetContext.put(fieldName,true);
                    break;
                case BooleanValue:
                    targetContext.put(fieldValue,true);
                    break;
                case Position:
                    targetContext.put(fieldName,m.start());
                    break;
                case String:
                    targetContext.put(fieldName,targetContext.optString(fieldName)+System.lineSeparator()+fieldValue);
                    break;
                case List:
                    defaut:
                    targetContext.accumulate(fieldName,fieldValue);
                    break;
            }
        }
        return changedContext;
    }
    public boolean test(CharSequence input){
        matcher.reset(input);
        return matcher.find();
    }
    public boolean apply(CheatChars line, JsonBuilder builder, Parser parser){
        boolean result = applyWithStart(line,builder,parser,0);
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

    private boolean applyWithStart(CheatChars line,JsonBuilder builder,Parser parser,int start){

        //TODO enable disable Exp with a boolean state? allows state dependent parsers similar to child parsers but for other lines
        //Parser would maintain the "state" of what is enabled / disabled and the expression would check if it is enabled

        if(!this.requires.isEmpty()){
            boolean satisfyRequired = true;
            for(String required : requires){
                satisfyRequired = satisfyRequired & parser.getState(required);
            }
            if( !satisfyRequired ){
                return false;
            }
        }

        if(isDebug()){
            System.out.println(this.getName()+" "+start+": line = "+line);
            System.out.println(builder.getRoot().toString(2));
        }
        boolean rtrn = false;
        matcher.reset(line);
        int startPoint = is(Rule.LineStart) ? 0 : start;
        if(isDebug()){
            System.out.printf("%10s startPoint=%d\n",this.getName(),startPoint);
        }
        matcher.region( startPoint,line.length() );

        if(matcher.find()){
            if(isDebug()){
                System.out.printf("%10s found match\n",this.getName());
            }

            rtrn = true;
            if ( is(Merge.NewStart) ) {
                if(isDebug()){System.out.printf("%10s NewStart\n",this.getName());}
                builder.close();
            }else if( is(Rule.AvoidContext) ) {
                if(isDebug()){System.out.printf("%10s AvoidContext\n",this.getName());}
                builder.popContext();
            }

            JSONObject context = builder.getCurrentContext();
            JSONObject target = context;

            do {
                target = context;
                JSONObject grouped = target;

                for(Iterator<String> groupIter = grouping.keySet().iterator(); groupIter.hasNext();){
                    String groupName = groupIter.next();
                    GroupType groupType = grouping.get(groupName);
                    boolean extend = false;
                    if(isDebug()){
                        System.out.printf("%10s grouping %s = %s\n",this.getName(),groupType,groupName);
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
                                    System.out.printf("%10s already has %s\n",this.getName(),groupName);
                                }
                                JSONObject entry = new JSONObject();
                                grouped.append(groupName,entry);
                                grouped = entry;
                            } else {
                                if(isDebug()){
                                    System.out.printf("%10s does not have %s\n",this.getName(),groupName);
                                }
                                JSONObject entry = new JSONObject();
                                JSONArray arry = new JSONArray();
                                arry.put(entry);
                                grouped.put(groupName,arry);
                                grouped = entry;
                            }
                        } else if ( is(Merge.Extend) ) {
                            if( grouped.has(groupName) ) {
                                JSONArray arry = grouped.getJSONArray(groupName);
                                JSONObject last = arry.getJSONObject(arry.length()-1);
                                grouped = last;
                            }else{
                                JSONObject entry = new JSONObject();
                                JSONArray arry = new JSONArray();
                                arry.put(entry);
                                grouped.put(groupName,arry);
                                grouped = entry;
                            }

                        } else if ( is(Merge.Collection) ) {
                            if( grouped.has(groupName) ) {
                                grouped = grouped.getJSONObject(groupName);
                            } else {
                                JSONObject newJSON = new JSONObject();
                                grouped.put(groupName,newJSON);
                                grouped = newJSON;
                            }
                        } else {
                            //will happen if NewEntry, no action because new-entry merges with the current context :)
                            if(isDebug()){
                                System.out.printf("%10s new entry automatically merges with existing context",this.getName());
                            }
                        }
                    } else {
                        //same behavior as Merge.Collection
                        if(grouped.has(groupName)){
                            //could be an array
                            Object obj = grouped.get(groupName);
                            if(obj instanceof JSONArray){
                                JSONArray groupArry = (JSONArray)obj;
                                if( (extend || is(Merge.Extend)) && groupArry.length()>0){
                                    grouped = groupArry.getJSONObject(groupArry.length()-1);
                                }else{
                                    JSONObject newInstance = new JSONObject();
                                    groupArry.put(newInstance);
                                    grouped = newInstance;
                                }
                            }else {
                                grouped = grouped.getJSONObject(groupName);
                            }
                        }else{
                            JSONObject newJSON = new JSONObject();
                            grouped.put(groupName,newJSON);
                            grouped = newJSON;
                        }
                    }
                }

                boolean needPop = false;
                if(target != grouped){
                    target = grouped; // will only change target if there was a grouping
                    builder.setCurrentContext(target);
                    needPop = true;
                }

                boolean changedContext = populate(matcher,builder);

                if( changedContext ){//currently only happens for Value.KeyLength
                    JSONObject cc = builder.getCurrentContext();


                    target = cc;
                }

                Eat toEat = Eat.from(this.eat);
                int mStart = matcher.start();
                int mEnd = matcher.end();
                switch(toEat){
                    case Match:
                        int mStop = matcher.end();
                        line.drop(mStart,mStop);
                        matcher.region(mStart,line.length());
                        mEnd = mStart;
                        break;
                    case Width:
                        int wStop = this.eat;
                        line.drop(mStart,wStop);
                        matcher.region(mStart,line.length());
                        mEnd = mStart;
                        break;
                }

                //call each child
                for(Exp child : children){
                    child.applyWithStart(line,builder,parser,mEnd);
                }

                if(needPop && !changedContext) { //TODO I'm sure this breaks NestLength
                    builder.popContext();//pop target
                }
                //only notify the callbacks for the last occurrence of a match
                if(!is(Rule.Repeat)) {
                    for (MatchAction action : callbacks) {
                        action.onMatch(target, this, parser);
                    }
                }

            }while( is(Rule.Repeat) && matcher.find() );

            //only notify the callbacks for the last occurrence of a match
            if( is(Rule.Repeat) && rtrn){
                for (MatchAction action : callbacks) {
                    action.onMatch(target, this, parser);
                }
            }

            //update the parser states after looping
            if(!disables.isEmpty()){
                disables.forEach((disable)->parser.setState(disable,false));
            }
            if(!enables.isEmpty()){
                enables.forEach(((enable)->parser.setState(enable,true)));
            }

            if( is(Rule.PopContext) ) {
                builder.popContext();
            }
            if( is(Rule.ClearContext) ) {
                builder.clearContext();
            }
            if( is(Rule.PushContext) ) {
                builder.setCurrentContext(target);
            }

            if(Eat.from(this.eat) == Eat.Line){ // eat the line after applying children and repeating
                line.drop(0,line.length());
            }

        }//matched

        return rtrn;
    }
    public static String pad(int i){
        if(i<=0)
            return "";
        return "                                                                                                                                                                                                              ".substring(0,i);
    }
}
