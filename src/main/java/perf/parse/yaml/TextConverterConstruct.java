package perf.parse.yaml;

import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.*;
import perf.parse.Exp;
import perf.parse.file.TextConverter;
import perf.yaup.yaml.DeferableConstruct;

public class TextConverterConstruct extends DeferableConstruct {
    @Override
    public Object construct(Node node) {
        TextConverter rtrn = new TextConverter();
        if(node instanceof ScalarNode){
            //rtrn.addPreset(preset);//TODO find the parser to add
        }else if (node instanceof SequenceNode){
            ((SequenceNode)node).getValue().forEach(entryNode->{
                Object deferred = super.deferAs(entryNode,new Tag(Exp.class));
                if(deferred instanceof Exp){
                    rtrn.addExp((Exp)deferred);
                }else{
                    throw new YAMLException("failed to create an Exp"+entryNode.getStartMark());
                }
            });
        }else if (node instanceof MappingNode){
            MappingNode mappingNode = (MappingNode)node;
            mappingNode.getValue().forEach(nodeTuple -> {
                if (!(nodeTuple.getKeyNode() instanceof ScalarNode)) {
                    throw new YAMLException("TextConverter keys must be scalar" + nodeTuple.getKeyNode().getStartMark());
                }
                String key = ((ScalarNode) nodeTuple.getKeyNode()).getValue();
                Node valueNode = nodeTuple.getValueNode();
                switch (key.toLowerCase()) {
                    case "parser":
                        if(valueNode instanceof ScalarNode){
                            //rtrn.addPreset(preset);//add a named parser
                        }else if (valueNode instanceof SequenceNode){
                            ((SequenceNode)valueNode).getValue().forEach(entryNode->{
                                if(entryNode instanceof ScalarNode){
                                    //rtrn.addPreset(preset); todo add a named parser
                                }else{
                                    throw new YAMLException("TextConverter preset entries must be scalar"+entryNode.getStartMark());
                                }
                            });
                        }
                        break;
                    case "exp":
                        if(valueNode instanceof SequenceNode){
                            ((SequenceNode)valueNode).getValue().forEach(entryNode->{
                                Object deferred = super.deferAs(entryNode,new Tag(Exp.class));
                                if(deferred instanceof Exp){
                                    rtrn.addExp((Exp)deferred);
                                }else{
                                    throw new YAMLException("failed to create an Exp"+entryNode.getStartMark());
                                }
                            });
                        }else {
                            Object deferred = super.deferAs(valueNode,new Tag(Exp.class));
                            if(deferred instanceof Exp){
                                rtrn.addExp((Exp)deferred);
                            } else {
                                throw new YAMLException("failed to create an Exp" + valueNode.getStartMark());
                            }
                        }
                        break;
                }
            });
        }


        return rtrn;
    }
}
