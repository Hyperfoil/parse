package io.hyperfoil.tools.parse.yaml;

import io.hyperfoil.tools.parse.file.*;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.*;
import io.hyperfoil.tools.yaup.yaml.DeferableConstruct;

import java.util.List;
import java.util.stream.Collectors;

public class FileRuleConstruct extends DeferableConstruct {
    @Override
    public Object construct(Node node) {
        FileRule rtrn = new FileRule();
        if(node instanceof MappingNode){
            MappingNode mappingNode = (MappingNode)node;
            mappingNode.getValue().forEach(nodeTuple -> {
                if (!(nodeTuple.getKeyNode() instanceof ScalarNode)) {
                    throw new YAMLException("FileRule keys must be scalar" + nodeTuple.getKeyNode().getStartMark());
                }
                String key = ((ScalarNode) nodeTuple.getKeyNode()).getValue();
                Node valueNode = nodeTuple.getValueNode();
                switch (key.toLowerCase()) {
                    case "match":
                        if(valueNode instanceof SequenceNode){
                            ((SequenceNode)valueNode).getValue().forEach(entryNode->{
                                Object deferred = deferAs(entryNode,new Tag(MatchCriteria.class));
                                if(deferred instanceof MatchCriteria){
                                    rtrn.setCriteria((MatchCriteria)deferred);
                                }else{
                                    throw new YAMLException("failt to create a MatchCriteria"+entryNode.getStartMark());
                                }
                            });
                        }else{
                            Object deferred = deferAs(valueNode,new Tag(MatchCriteria.class));
                            if(deferred instanceof MatchCriteria){
                                rtrn.setCriteria((MatchCriteria)deferred);
                            }else {
                                throw new YAMLException("failt to create a MatchCriteria" + valueNode.getStartMark());
                            }
                        }
                        break;
                    case "nest":
                        if(valueNode instanceof ScalarNode){
                            rtrn.setNest(((ScalarNode)valueNode).getValue());
                        }else{
                            throw new YAMLException("FileRule nest must be a scalar"+valueNode.getStartMark());
                        }
                        break;
                    case "parse":
                        if(valueNode instanceof SequenceNode){
                            String value = ((ScalarNode)valueNode).getValue().toLowerCase().trim();
                            if(value.equals("json")){
                                rtrn.setConverter(new JsonConverter());
                            }else if (value.equals("xml")){
                                rtrn.setConverter(new XmlConverter());
                            }else if (value.equals("jboss-cli") || value.equals("jbosscli")){
                                rtrn.setConverter(new JbossCliConverter());
                            }else{
                                throw new YAMLException("failed to create a parser"+valueNode.getStartMark());
                            }
                        }else if (valueNode instanceof MappingNode){
                            MappingNode valueMapping = (MappingNode)valueNode;
                            List<String> keys = valueMapping.getValue().stream().map(tuple->((ScalarNode)tuple.getKeyNode()).getValue()).collect(Collectors.toList());
                            if(keys.size()>1){
                                throw new YAMLException("FileRule parse can only have one key mapping"+valueNode.getStartMark());
                            }
                            String converterKey = keys.get(0).toLowerCase().trim();
                            Node mappingValue = ((MappingNode) valueNode).getValue().get(0).getValueNode();

                            deferAs(mappingNode,new Tag(converterKey));

                        }
                        break;
                    case "filter":
                        if(valueNode instanceof SequenceNode){
                            ((SequenceNode)valueNode).getValue().forEach(entryNode->{
                                Object deferred = deferAs(entryNode,new Tag(Filter.class));
                                if(deferred instanceof Filter){
                                    rtrn.addFilter((Filter)deferred);
                                }else{
                                    throw new YAMLException("failed to create a filter"+entryNode.getStartMark());
                                }
                            });
                        }else{
                            Object deferred = deferAs(valueNode,new Tag(Filter.class));
                            if(deferred instanceof Filter){
                                rtrn.addFilter((Filter)deferred);
                            }else{
                                throw new YAMLException("failed to create a filter"+valueNode.getStartMark());
                            }
                        }
                        break;
                }
            });
        }else{
            throw new YAMLException("FileRule requires a mapping"+node.getStartMark());
        }
        return rtrn;
    }
}
