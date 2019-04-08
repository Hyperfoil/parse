package perf.parse;

import perf.parse.internal.JsonBuilder;
import perf.yaup.json.Json;

import java.util.List;
import java.util.stream.Collectors;

public enum MatchRule {
   Repeat,
   RepeatChildren,
   PushTarget,
   PreClose,
   PostClose,
   PrePopTarget,
   PostPopTarget,
   PreClearTarget,
   PostClearTarget,
   TargetRoot;


   public boolean prePopulate(JsonBuilder builder, List<Object> data){
      boolean changedTarget = false;
      List<Object> filteredData = filterRules(data);
      switch (this){
         case PreClose:
            builder.close();
            changedTarget=true;
            break;
         case PrePopTarget:
            System.out.println(">>PrePopTarget\n"+builder.debug(true));
            if(filteredData.isEmpty()){
               builder.popTarget();
            }else{
               filteredData.forEach(name->builder.popTarget(name.toString()));
            }
            changedTarget=true;
            System.out.println("<<PrePopTarget\n"+builder.debug(true));
            break;
         case PreClearTarget:
            if (filteredData.isEmpty()) {
               builder.clearTargets();
            }else{
               filteredData.forEach(name->builder.clearTargets(name.toString()));
            }
            changedTarget=true;
            break;
         case TargetRoot:
            System.out.println("  "+toString());
            builder.pushTarget(builder.getRoot(),filteredData.toString()+ Exp.ROOT_TARGET_NAME);
            System.out.println(builder.getRoot().toString());
            break;
      }
      return changedTarget;
   }
   public boolean preChildren(JsonBuilder builder, Json target, List<Object> data){
      boolean changedTarget = false;
      List<Object> filteredData = filterRules(data);
      switch (this){
         case PushTarget:
            if(filteredData.isEmpty()){
               builder.pushTarget(target);
            }else{
               filteredData.forEach(name->builder.pushTarget(target,name.toString()));
            }
            changedTarget = true;
            break;
      }
      return changedTarget;
   }

   public boolean postChildren(JsonBuilder builder,Json target,List<Object> data){
      boolean changedTarget = false;
      List<Object> filteredData = filterRules(data);
      switch (this){
         case PostClose:
            builder.close();
            changedTarget = true;
            break;
         case PostPopTarget:
            System.out.println(">>PostPopTarget "+data+"\n"+builder.debug(true));
            if(filteredData.isEmpty()){
               builder.popTarget(data.size());
            }else{
               filteredData.forEach(name->builder.popTarget(name.toString()));
            }
            changedTarget = true;
            System.out.println("<<PostPopTarget\n"+builder.debug(true));
            break;
         case PostClearTarget:
            System.out.println(">>PostClearTarget "+data+"\n"+builder.debug(true));
            if(filteredData.isEmpty()){
               builder.clearTargets();
            }else{
               filteredData.forEach(name->builder.clearTargets(name.toString()));
            }
            System.out.println("<<PostClearTarget "+data+builder.debug(true));
            changedTarget=true;
         break;
         case TargetRoot:
            System.out.println("  "+toString());
            builder.popTarget(filteredData.toString()+ Exp.ROOT_TARGET_NAME);
            System.out.println(builder.getRoot().toString());
            break;
      }
      return changedTarget;
   }

   public List<Object> filterRules(List<Object> data){
      return data.stream().filter(v->!(v instanceof MatchRule)).collect(Collectors.toList());
   }
}
