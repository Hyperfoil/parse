package perf.parse.yaml;

import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;
import perf.parse.*;
import perf.yaup.StringUtil;
import perf.yaup.json.Json;
import perf.yaup.yaml.DeferableConstruct;

import java.util.function.BiConsumer;

import static perf.yaup.yaml.OverloadConstructor.json;

public class ExpConstruct extends DeferableConstruct {


    @Override
    public Object construct(Node node) {

        BiConsumer<Exp,ScalarNode> ruleScalar = (e, n)->{
            try {
                e.setRule(StringUtil.getEnum(n.getValue(),MatchRule.class,null));
            }catch(IllegalArgumentException iae){
                throw new YAMLException("invalid rule value"+n.getStartMark());
            }
        };
        BiConsumer<Exp,MappingNode> ruleMap = (e, n)->{
            n.getValue().forEach(rule->{
                if( !(rule.getKeyNode() instanceof ScalarNode) ){
                    throw new YAMLException("rule mapping requires a scalar key"+rule.getKeyNode().getStartMark());
                }
                if( !(rule.getValueNode() instanceof ScalarNode) ){
                    throw new YAMLException("rule mapping requires a scalar value"+rule.getValueNode().getStartMark());
                }
                String key = ((ScalarNode)rule.getKeyNode()).getValue();
                String value = ((ScalarNode)rule.getValueNode()).getValue();
                try{
                    e.setRule(StringUtil.getEnum(key,MatchRule.class,null),value);
                }catch (IllegalArgumentException iae){
                    throw new YAMLException("invalid rule mapping key"+rule.getKeyNode().getStartMark());
                }
            });
        };
        if(node instanceof ScalarNode){
            String pattern = ((ScalarNode)node).getValue();
            return new Exp("exp-"+System.currentTimeMillis(),pattern);
        }else if(node instanceof MappingNode){
            MappingNode mappingNode = (MappingNode)node;
            Json json = json(node);
            Exp rtrn = null;
            if(json.has("name") && json.has("pattern")){
                rtrn = new Exp(json.getString("name"),json.getString("pattern"));
            }else{
                throw new YAMLException("Exp requires Name and pattern"+node.getStartMark());
            }
            if(rtrn!=null){
                final Exp exp = rtrn;
                mappingNode.getValue().forEach(nodeTuple -> {
                    if(!( nodeTuple.getKeyNode() instanceof ScalarNode) ){
                        throw new YAMLException("Exp keys must be scalar "+nodeTuple.getKeyNode().getStartMark());
                    }
                    String key = ((ScalarNode)nodeTuple.getKeyNode()).getValue();
                    Node valueNode = nodeTuple.getValueNode();
                    switch (key){
                        case "debug":
                            //exp.debug();
                            break;
                        case "name":
                        case "pattern":
                            break;
                        case "eat":
                            if(valueNode instanceof ScalarNode){
                                String value = ((ScalarNode)valueNode).getValue();
                                if(value.matches("//d+")){
                                    exp.eat(Integer.parseInt(value));
                                }else{
                                    try {
                                        exp.eat(StringUtil.getEnum(value,Eat.class,Eat.Match));
                                    }catch(IllegalArgumentException e){
                                        throw new YAMLException("invalid eat value"+valueNode.getStartMark());
                                    }
                                }
                            }else{
                                throw new YAMLException("eat must be a scalar"+valueNode.getStartMark());
                            }
                            break;
                        case "children":
                            if(valueNode instanceof SequenceNode){
                                SequenceNode children = (SequenceNode)valueNode;
                                children.getValue().forEach((child)->{
                                    Object childExp = construct(child);
                                    if(childExp!=null && childExp instanceof Exp){
                                        exp.add((Exp)childExp);
                                    }
                                });

                            }else{
                                throw new YAMLException("children must be a sequence"+valueNode.getStartMark());
                            }
                            break;
                        case "merge":
                            if(valueNode instanceof ScalarNode){
                                try {
                                    ExpMerge m = StringUtil.getEnum(((ScalarNode) valueNode).getValue(),ExpMerge.class, ExpMerge.ByKey);
                                    exp.setMerge(m);
                                }catch(IllegalArgumentException e){
                                    throw new YAMLException("invalid merge value"+valueNode.getStartMark());
                                }
                            }else{
                                throw new YAMLException("merge must be a scalar"+valueNode.getStartMark());
                            }

                            break;
                        case "requires":
                            if(valueNode instanceof ScalarNode){
                                exp.requires(((ScalarNode)valueNode).getValue());
                            }else if (valueNode instanceof SequenceNode){
                                SequenceNode sequenceNode = (SequenceNode)valueNode;
                                sequenceNode.getValue().forEach((entry)->{
                                    if(entry instanceof ScalarNode){
                                        exp.requires(((ScalarNode)entry).getValue());
                                    }else{
                                        throw new YAMLException("requires entries must be scalar"+entry.getStartMark());
                                    }
                                });
                            }else{
                                throw new YAMLException("requires should be a scalar or sequence"+valueNode.getStartMark());
                            }
                            break;
                        case "enables":
                            if(valueNode instanceof ScalarNode){
                                exp.enables(((ScalarNode)valueNode).getValue());
                            }else if (valueNode instanceof SequenceNode){
                                SequenceNode sequenceNode = (SequenceNode)valueNode;
                                sequenceNode.getValue().forEach((entry)->{
                                    if(entry instanceof ScalarNode){
                                        exp.enables(((ScalarNode)entry).getValue());
                                    }else{
                                        throw new YAMLException("enables entries must be scalar"+entry.getStartMark());
                                    }
                                });
                            }else{
                                throw new YAMLException("enables should be a scalar or sequence"+valueNode.getStartMark());
                            }
                            break;
                        case "disables":
                            if(valueNode instanceof ScalarNode){
                                exp.disables(((ScalarNode)valueNode).getValue());
                            }else if (valueNode instanceof SequenceNode){
                                SequenceNode sequenceNode = (SequenceNode)valueNode;
                                sequenceNode.getValue().forEach((entry)->{
                                    if(entry instanceof ScalarNode){
                                        exp.disables(((ScalarNode)entry).getValue());
                                    }else{
                                        throw new YAMLException("disables entries must be scalar"+entry.getStartMark());
                                    }
                                });
                            }else{
                                throw new YAMLException("disables should be a scalar or sequence"+valueNode.getStartMark());
                            }
                            break;
                        case "rule":
                            if(valueNode instanceof ScalarNode){
                                ruleScalar.accept(exp,(ScalarNode)valueNode);
                            }else if (valueNode instanceof MappingNode){
                                ruleMap.accept(exp,(MappingNode)valueNode);
                            }else if (valueNode instanceof SequenceNode){
                                SequenceNode sequenceNode = (SequenceNode)valueNode;
                                sequenceNode.getValue().forEach(entry->{
                                    if(entry instanceof ScalarNode){
                                        ruleScalar.accept(exp,(ScalarNode)entry);
                                    }else if (entry instanceof MappingNode){
                                        ruleMap.accept(exp,(MappingNode)entry);
                                    }else{
                                        throw new YAMLException("rule sequence entreis must be scalar or mapping"+entry.getStartMark());
                                    }
                                });
                            }else{
                                throw new YAMLException("rule must be a scalar, sequence, or mapping"+valueNode.getStartMark());
                            }
                            break;
                        case "nest":
                            if(valueNode instanceof ScalarNode){
                                exp.nest(((ScalarNode)valueNode).getValue());
                            }else{
                                throw new YAMLException("nest requires a scalar"+valueNode.getStartMark());
                            }
                            break;
                        case "group":
                            if(valueNode instanceof ScalarNode){

                                exp.group(((ScalarNode)valueNode).getValue());
                            }else if (valueNode instanceof SequenceNode){
                                ((SequenceNode)valueNode).getValue().forEach((entry)->{
                                    if(entry instanceof ScalarNode){
                                        exp.group(((ScalarNode)valueNode).getValue());
                                    }else{
                                        throw new YAMLException("group sequence entries need to be scalar"+entry.getStartMark());
                                    }
                                });
                            }else {
                                throw new YAMLException("group requires a scalar or sequence"+valueNode.getStartMark());
                            }
                            break;
                        case "key":
                            if(valueNode instanceof ScalarNode){

                                exp.key(((ScalarNode)valueNode).getValue());
                            }else if (valueNode instanceof SequenceNode){
                                ((SequenceNode)valueNode).getValue().forEach((entry)->{
                                    if(entry instanceof ScalarNode){
                                        exp.key(((ScalarNode)valueNode).getValue());
                                    }else{
                                        throw new YAMLException("key sequence entries need to be scalar"+entry.getStartMark());
                                    }
                                });
                            }else {
                                throw new YAMLException("key requires a scalar or sequence"+valueNode.getStartMark());
                            }
                            break;
                        case "extend":
                            if(valueNode instanceof ScalarNode){

                                exp.extend(((ScalarNode)valueNode).getValue());
                            }else if (valueNode instanceof SequenceNode){
                                ((SequenceNode)valueNode).getValue().forEach((entry)->{
                                    if(entry instanceof ScalarNode){
                                        exp.extend(((ScalarNode)valueNode).getValue());
                                    }else{
                                        throw new YAMLException("extend sequence entries need to be scalar"+entry.getStartMark());
                                    }
                                });
                            }else {
                                throw new YAMLException("extend requires a scalar or sequence"+valueNode.getStartMark());
                            }
                            break;
                    }

                });


            }
            return rtrn;
        }else{
            throw new YAMLException("Exp requires a mapping node"+node.getStartMark());
        }
    }
}
