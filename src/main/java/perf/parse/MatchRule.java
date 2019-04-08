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
            if(filteredData.isEmpty()){
               builder.popTarget();
            }else{
               filteredData.forEach(name->builder.popTarget(name.toString()));
            }
            changedTarget=true;
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
            builder.pushTarget(builder.getRoot(),filteredData.toString()+ Exp.ROOT_TARGET_NAME);
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
            if(filteredData.isEmpty()){
               builder.popTarget(data.size());
            }else{
               filteredData.forEach(name->builder.popTarget(name.toString()));
            }
            changedTarget = true;
            break;
         case PostClearTarget:
            if(filteredData.isEmpty()){
               builder.clearTargets();
            }else{
               filteredData.forEach(name->builder.clearTargets(name.toString()));
            }
            changedTarget=true;
         break;
         case TargetRoot:
            builder.popTarget(filteredData.toString()+ Exp.ROOT_TARGET_NAME);
            break;
      }
      return changedTarget;
   }

   public List<Object> filterRules(List<Object> data){
      return data.stream().filter(v->!(v instanceof MatchRule)).collect(Collectors.toList());
   }
}
