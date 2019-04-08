package perf.parse.yaml;

import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;
import perf.parse.file.Filter;
import perf.yaup.yaml.DeferableConstruct;

public class FilterConstruct extends DeferableConstruct {
    @Override
    public Object construct(Node node) {
        Filter rtrn = new Filter();
        if(node instanceof ScalarNode){
            String value = ((ScalarNode)node).getValue();
            if(value.startsWith("function") || value.contains("=>")){

                rtrn.setResult(value);
            }else{
                throw new YAMLException("scalar filter defintion must be a javascript function"+node.getStartMark());
            }
        }else if (node instanceof MappingNode){
            MappingNode mappingNode = (MappingNode)node;
            mappingNode.getValue().forEach(nodeTuple -> {
                if(! (nodeTuple.getKeyNode() instanceof ScalarNode)){
                    throw new YAMLException("filter keys must be scalar"+nodeTuple.getKeyNode().getStartMark());
                }
                String key = ((ScalarNode)nodeTuple.getKeyNode()).getValue();
                Node valueNode = nodeTuple.getValueNode();
                switch (key.toLowerCase()){
                    case "path":
                        String path = ((ScalarNode)valueNode).getValue();
                        rtrn.setPath(path);
                        if(path.startsWith("/")){
                            rtrn.setType(Filter.Type.Xml);
                        }else if (path.startsWith("$")){
                            rtrn.setType(Filter.Type.Json);
                        }else{
                            throw new YAMLException("valid filter paths start with / or $"+valueNode.getStartMark());
                        }
                        break;
                    case "nest":
                        rtrn.setNest(((ScalarNode)valueNode).getValue());
                        break;
                    case "regex":
                        rtrn.setRegex(((ScalarNode)valueNode).getValue());
                        break;
                    case "result":
                        rtrn.setResult(((ScalarNode)valueNode).getValue());
                        break;
                }
            });
        }
        return rtrn;
    }
}
