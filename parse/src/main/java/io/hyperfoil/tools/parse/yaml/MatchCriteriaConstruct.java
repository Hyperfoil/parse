package io.hyperfoil.tools.parse.yaml;

import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;
import io.hyperfoil.tools.parse.file.MatchCriteria;
import io.hyperfoil.tools.yaup.yaml.DeferableConstruct;

public class MatchCriteriaConstruct extends DeferableConstruct {
    @Override
    public Object construct(Node node) {
        MatchCriteria rtrn = new MatchCriteria();

        if(node instanceof ScalarNode){
            rtrn.setPathPattern(((ScalarNode)node).getValue());
        }else if (node instanceof MappingNode){
            MappingNode mappingNode = (MappingNode)node;
            mappingNode.getValue().forEach(nodeTuple -> {
                if(!( nodeTuple.getKeyNode() instanceof ScalarNode) ){
                    throw new YAMLException("MatchCriteria keys must be scalar"+nodeTuple.getKeyNode().getStartMark());
                }
                String key = ((ScalarNode)nodeTuple.getKeyNode()).getValue();
                Node valueNode = nodeTuple.getValueNode();
                switch (key.toLowerCase()) {
                    case "path":
                        if(valueNode instanceof ScalarNode){
                            rtrn.setPathPattern(((ScalarNode)valueNode).getValue());
                        }else{
                            throw new YAMLException("MatchCriteria path must be scalar"+valueNode.getStartMark());
                        }
                        break;
                    case "lines":
                        if(valueNode instanceof ScalarNode){
                            String value = ((ScalarNode)valueNode).getValue();
                            if(value.matches("\\d+")){
                                rtrn.setHeaderLines(Integer.parseInt(value));
                            }else{
                                throw new YAMLException("MatchCriteria lines must be a number"+valueNode.getStartMark());
                            }
                        }else{
                            throw new YAMLException("MatchCriteria lines must be a number"+valueNode.getStartMark());
                        }
                        break;
                    case "find":
                        if(valueNode instanceof ScalarNode){
                            rtrn.addFindPattern(((ScalarNode)valueNode).getValue());
                        }else if (valueNode instanceof SequenceNode){
                            ((SequenceNode)valueNode).getValue().forEach(valueEntry->{
                                if(valueEntry instanceof ScalarNode){
                                    rtrn.addFindPattern(((ScalarNode)valueEntry).getValue());
                                }else{
                                    throw new YAMLException("MatchCriteria find list must be scalars"+valueEntry.getStartMark());
                                }
                            });
                        }else{
                            throw new YAMLException("MatchCriteria find must be scalar or a sequence of scalars"+valueNode.getStartMark());
                        }
                        break;
                    case "notfind":
                    case "not-find":
                        if(valueNode instanceof ScalarNode){
                            rtrn.addNotFindPattern(((ScalarNode)valueNode).getValue());
                        }else if (valueNode instanceof SequenceNode){
                            ((SequenceNode)valueNode).getValue().forEach(valueEntry->{
                                if(valueEntry instanceof ScalarNode){
                                    rtrn.addNotFindPattern(((ScalarNode)valueEntry).getValue());
                                }else{
                                    throw new YAMLException("MatchCriteria not-find list must be scalars"+valueEntry.getStartMark());
                                }
                            });
                        }else{
                            throw new YAMLException("MatchCriteria not-find must be scalar or a sequence of scalars"+valueNode.getStartMark());
                        }
                        break;
                }
            });
        }else{
            throw new YAMLException("MatchCriteria require a scalar or mapping"+node.getStartMark());
        }


        return rtrn;
    }
}
