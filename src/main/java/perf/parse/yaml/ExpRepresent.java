package perf.parse.yaml;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.nodes.*;
import org.yaml.snakeyaml.representer.Represent;
import perf.parse.Eat;
import perf.parse.ExpOld;

import java.util.LinkedList;
import java.util.List;
import java.util.function.BiFunction;

public class ExpRepresent implements Represent {
    @Override
    public Node representData(Object data) {
        BiFunction<String,String,NodeTuple> stringTuple = (k,v)->{
            return new NodeTuple(
                new ScalarNode(Tag.STR,k,null,null, DumperOptions.ScalarStyle.PLAIN),
                new ScalarNode(Tag.STR,v,null,null, DumperOptions.ScalarStyle.PLAIN)
            );
        };
        List<NodeTuple> tupleList = new LinkedList<>();
        if(data instanceof ExpOld){
            ExpOld exp = (ExpOld)data;
            tupleList.add(stringTuple.apply("Name",exp.getName()));
            tupleList.add(stringTuple.apply("pattern",exp.getPattern()));
            tupleList.add(stringTuple.apply("merge",exp.getMerge().name()));
            if(Eat.Width.equals(Eat.from(exp.getEat()))){
                tupleList.add(stringTuple.apply("eat",""+exp.getEat()));
            }else{
                tupleList.add(stringTuple.apply("eat",Eat.from(exp.getEat()).name()));
            }

            if(exp.isGrouped()){
                exp.eachGroup((k,v)->{
                    switch (v){
                        case Name:
                            tupleList.add(stringTuple.apply("group",k));
                            break;
                        case Extend:
                            tupleList.add(stringTuple.apply("extend",k));
                            break;
                        case Field:
                            tupleList.add(stringTuple.apply("key",k));
                        default:
                            //TODO What grouping is this?
                    }
                });
            }
            if(exp.hasRules()){
                List<Node> ruleList = new LinkedList<>();
                exp.eachRule((rule,list)->{
                    list.forEach(entry->{
                        if(entry.equals(rule)){
                            ruleList.add(new ScalarNode(Tag.STR,rule.name(),null,null, DumperOptions.ScalarStyle.PLAIN));
                        }else{
                            List<NodeTuple> entryMap = new LinkedList<>();
                            entryMap.add(stringTuple.apply(rule.name(),entry.toString()));
                            ruleList.add(new MappingNode(Tag.MAP,entryMap, DumperOptions.FlowStyle.FLOW));
                        }
                    });
                });
                tupleList.add(
                    new NodeTuple(
                        new ScalarNode(Tag.STR,"rules",null,null, DumperOptions.ScalarStyle.PLAIN),
                        new SequenceNode(Tag.SEQ,ruleList, DumperOptions.FlowStyle.BLOCK)
                    )
                );
            }
            if(exp.hasChildren()){
                List<Node> childrenList = new LinkedList<>();
                exp.eachChild((child)->{
                    Node childNode = representData(child);
                    childrenList.add(childNode);
                });
                tupleList.add(
                    new NodeTuple(
                        new ScalarNode(Tag.STR,"children",null,null,DumperOptions.ScalarStyle.PLAIN),
                        new SequenceNode(Tag.SEQ,childrenList, DumperOptions.FlowStyle.BLOCK)
                    )
                );
            }
        }
        return new MappingNode(Tag.MAP,tupleList, DumperOptions.FlowStyle.AUTO);
    }
}
