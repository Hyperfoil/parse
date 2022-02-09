package io.hyperfoil.tools.parse;

import io.hyperfoil.tools.parse.json.JsonBuilder;
import io.hyperfoil.tools.yaup.json.Json;

import java.util.concurrent.atomic.AtomicBoolean;

public enum ValueMerge {
    /**
     * Sets key = value if key is not defined, otherwise key = [value,...]
     */
    Auto(false,(key,value,builder,data)->{
       Json.chainAct(builder.getTarget(),key,value,(target,k,v)->{
          if(!target.has(k)){
             target.set(k,v);
          }else if (target.get(k) instanceof Json && target.getJson(k).isArray()){
             target.getJson(k).add(v);
          }else{
             Json newArry = new Json();
             newArry.add(target.get(k));
             newArry.add(v);
             target.set(k,newArry);
          }
       });
        return false;
    }),
    /**
     * Sets key = true
     */
    BooleanKey(false,(key,value,builder,data)->{
        Json.chainSet(builder.getTarget(),key,true);
        return false;
    }),
    /**
     * Sets value = true where value is from the input
     */
    BooleanValue(false,(key,value,builder,data)->{
        //Json.chainSet(builder.getTarget(),key,true);
        builder.getTarget().set(value,true);//chainSet value??
        return false;
    }),
    /**
     * Uses the value to determine if this is the start of a new json object
     * or if the values should merge into the existing object. A new object
     * is created if the current objects has a value defined and that value
     * is not equal to the value from the input
     */
    TargetId(true,(key,value,builder,data)->{
       AtomicBoolean rtrn = new AtomicBoolean(false);
       Object existing = builder.getContext("TargetId:"+key,true,null);
       if(existing==null){
           //TODO does setting targetId on parent context make sense for uses other than substratevm?
           //should the context be set on the target before grouping?
           builder.setContext("TargetId:"+key,value,-1);
       } else if (value.equals(existing)) {
           //same target, don't mutate
       }else{
           builder.close();
       }
       Json.chainAct(builder.getTarget(),key,value,(target,k,v)->{
           if(!target.has(k)){
               target.set(k,v);
           }else if (v.equals(target.get(k))){//same event

           }else{
               builder.close();
               Json.chainSet(builder.getTarget(),key,value);
               rtrn.set(true);
           }
       });
       return rtrn.get();
    }),
    /**
     * sets key = the number of times the patter has matched while building the current json object
     */
    Count((key,value,builder,data)->{
       Json.chainAct(builder.getTarget(),key,value,(target,k,v)->{
           if(target.has(v) && target.get(v) instanceof Long){
               target.set(v,target.getLong(v)+1);
           }else{
               target.set(v,1l);
           }
       });
        return false;
    }),
    /**
     * Add the value to any existing value already stored for key, converting to numbers if the value
     */
    Add((key, value, builder, data)->{
        Json.chainAct(builder.getTarget(),key,value,(target,k,v)->{
            if(v instanceof Number){
                target.set(k, ((Number)v).doubleValue() + target.getDouble(k, 0.0));
            }else{
                target.set(k,target.getString(k,"")+v.toString());
            }
        });

        return false;
    }),
    /**
     * Sets key = [value,...] even if only one value is found
     */
    List((key,value,builder,data)->{
        Json.chainAdd(builder.getTarget(),key,value);
        return false;
    }),
    /**
     * Uses the value from the input as the key for the value from the other specified capture group.
     * Can be specified in the capture name with {@literal(?<foo:key=bar>\\S+) (?<bar>\\S+)} to create json {biz:buz}
     * from input "biz buz"
     */
    Key((key,value,builder,data)->{
       Json.chainAct(builder.getTarget(),key,value,(target,k,v)->{
           if(target.has(v)){
               if(target.get(v) instanceof Json && target.getJson(v).isArray()){
                   target.getJson(v).add(data);
               }else{
                   Json newValue = new Json();
                   newValue.add(target.get(v));
                   newValue.add(data);
                   target.set(v,newValue);
               }
           }else{
               target.set(v,data);
           }
        });
       return false;
    }),
    /**
     * Sets key = [value,...] where the array only contains unique entries
     */
    Set((key,value,builder,data)->{
        Json.chainAct(builder.getTarget(),key,value,(target,k,v)->{
           if(!target.has(k)){
               target.set(k,new Json());
           }
           boolean haveIt = target.getJson(k).values().stream()
              .filter(existing->{
                  return existing.equals(v);

              })
              .findFirst()
              .orElse(null) != null;
           if(!haveIt){
               target.add(k,value);
           }
        });
        return false;
    }),
    /**
     * Sets key = value only if key is not defined on the current json object
     */
    First((key,value,builder,data)->{
        Json.chainAct(builder.getTarget(),key,value,(target,k,v)->{
            if(!target.has(k)){
                target.set(k,v);
            }
        });
        return false;
    }),
    /**
     * Sets key = value for each time the value is found in the input
     */
    Last((key,value,builder,data)->{
        Json.chainSet(builder.getTarget(),key,value);
        return false;
    }),
    /**
     * Uses value length to create a tree with key as the branch identifier.
     * Values with the same length are treated as sibling branches.
     */
    TreeSibling(true,new TreeMerger(false)),
    /**
     * TODO work in progress
     * Use value length to create a tree with key as the branch identifier.
     * Values with the same length are merged together if the values are equal
     */
    TreeMerging(true,new TreeMerger(true));

    private interface Merger {
        boolean merge(String key,Object value,JsonBuilder builder,Object data);
    }
    private static class TreeMerger implements Merger {

        final boolean merging;
        public TreeMerger(boolean merging){
            this.merging = merging;
        }

        @Override
        public boolean merge(String key, Object value, JsonBuilder builder,Object data) {
            boolean changedTarget = false;
            int valueLength = value.toString().length();
            int contextLength = builder.getContextInteger(key,true);
            if(builder.hasContext(key,true)){//the current context is already a tree
                if(valueLength > contextLength){
                    Json childAry = new Json();
                    Json newChild = new Json(false);
                    childAry.add(newChild);
                    builder.getTarget().add(key,childAry);
                    builder.pushTarget(childAry);
                    builder.setContext(key+ Exp.NEST_ARRAY,true);
                    builder.pushTarget(newChild);
                }else if (valueLength < contextLength){
                    while (valueLength < contextLength || builder.getContextBoolean(key+ Exp.NEST_ARRAY,false)){
                        builder.popTarget();//update changedTarget later in else if{...}
                        contextLength = builder.getContextInteger(key,true);
                    }
                    if(merging){
                        Json lastEntry = builder.getTarget().getJson(builder.getTarget().size()-1);
                        //TODO set the target?

                    }else{
                        builder.popTarget();//does this still break PrintGcFactoryTest.newParser_g1gc_details_nest
                        Json newEntry = new Json(false);
                        if(builder.getContextBoolean(key+ Exp.NEST_ARRAY,false)){
                            builder.getTarget().add(newEntry);
                        }else{
                            builder.getTarget().add(key,newEntry);
                            builder.pushTarget(builder.getTarget().getJson(key));
                            builder.setContext(key+ Exp.NEST_ARRAY,true);
                            builder.pushTarget(newEntry);//why double push newEntry?
                        }
                        builder.pushTarget(newEntry);//^^
                        changedTarget=true;
                    }
                }else{//sibling
                    if(merging){

                    }else{
                        while(builder.size()>1 && !builder.getContextBoolean(key+ Exp.NEST_ARRAY,false)){
                            builder.popTarget();
                            changedTarget=true;
                        }
                        Json newJson = new Json(false);
                        builder.getTarget().add(newJson);
                        builder.pushTarget(newJson);
                        changedTarget=true;

                    }
                }
            }else{//starting a tree
                Json treeArry = new Json(true);
                Json treeStart = new Json(false);
                treeArry.add(treeStart);
                builder.getTarget().add(key,treeArry);
                builder.pushTarget(treeArry);
                builder.setContext(key+ Exp.NEST_ARRAY,true);
                builder.pushTarget(treeStart);
                changedTarget=true;
            }
            builder.setContext(key,valueLength);
            if(changedTarget && !builder.hasContext(key+ Exp.NEST_VALUE,false)){
                builder.setContext(key+ Exp.NEST_VALUE,value);
            }

            return changedTarget;
        }
    }

    private final boolean isTargeting;
    private final Merger mergeFunction;

    ValueMerge(boolean isTargeting,Merger mergeFunction){
        this.isTargeting = isTargeting;
        this.mergeFunction = mergeFunction;
    }
    ValueMerge(Merger mergeFunction){
        this(false,mergeFunction);
    }

    public boolean isTargeting(){return isTargeting;}
    public boolean merge(String key,Object value,JsonBuilder builder,Object data){
        return mergeFunction.merge(key,value,builder,data);
    }

}
