package io.hyperfoil.tools.parse;

import io.hyperfoil.tools.parse.internal.JsonBuilder;
import io.hyperfoil.tools.yaup.json.Json;

import java.util.List;
import java.util.stream.Collectors;

public enum ExpRule {
   /**
    * Repeat the Exp and it's children until it no longer matches the input
    */
   Repeat,
   /**
    * Repeat all of the children until no children match the input
    */
   RepeatChildren,
   /**
    * Change the target json object to the current json object created from merging the Exp
    */
   PushTarget,
   /**
    * Close the current json objects and start a new json object before merging the Exp values.
    * A rule object changes the rule to first check if the current json object has the rule object as a key before closing
    */
   PreClose,
   /**
    * Close the current json objects and start a new json object after merging the Exp values
    * A rule object changes the rule to first check if the current json object has the rule object as a key before closing
    */
   PostClose,
   /**
    * Revert to the previous json target (or revert up to the named target) before merging the Exp values
    */
   PrePopTarget,
   /**
    * Revert to the previous json target (or revert up to the named target) after merging the Exp values
    */
   PostPopTarget,
   /**
    *
    */
   PreClearTarget,
   /**
    *
    */
   RemoveOnClose,
   PostClearTarget,
   TargetRoot;

   private interface RuleAction {
      boolean apply(JsonBuilder builder,Json target,List<Object> data);
   }
   private static final RuleAction DO_NOTHING = (b,t,d)->false;

   public boolean prePopulate(JsonBuilder builder, List<Object> data){
      boolean changedTarget = false;
      List<Object> filteredData = filterRules(data);
      switch (this){
         case PreClose:
            if(filteredData.isEmpty()){
               builder.close();
               changedTarget=true;
            }else{
               boolean shouldClose = filteredData.stream().filter(obj->builder.getTarget().has(obj)).findAny().isPresent();
               if(shouldClose){
                  builder.close();
                  changedTarget=true;
               }
            }
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
            if(filteredData.isEmpty()){
               builder.close();
               changedTarget=true;

            }else{
               boolean shouldClose = filteredData.stream().filter(obj->builder.getTarget().has(obj)).findAny().isPresent();
               if(shouldClose){
                  builder.close();
                  changedTarget=true;
               }

            }
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
      return data.stream().filter(v->!(v instanceof ExpRule) && v != null).collect(Collectors.toList());
   }
}
