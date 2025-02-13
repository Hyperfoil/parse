package io.hyperfoil.tools.parse.internal;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An implementation of CharSequence that supports removing from the middle of the sequence
 * and tracking changes to reference indexes
 */
public abstract class DropString implements CharSequence {


   public static class Drop {
      final int start;
      final int end;
      public Drop(int start,int stop){
         this.start = start;
         this.end = stop;
      }
      public int getStart(){return start;}
      public int getEnd(){return end;}
   }
   public static class Ref implements Comparable<Ref>{
      private final AtomicInteger value;
      private Ref(int index){
         value = new AtomicInteger(index);
      }
      public int get(){return value.get();}
      private void set(int newValue){
         value.set(newValue);
      }

      @Override
      public int compareTo(Ref o) {
         return Integer.compare(get(),o.get());
      }

   }

   String line;
   List<Ref> references = new LinkedList<>();
   List<Drop> drops = new ArrayList<>();

   DropString(String line){
      this.line = line;
   }

   public String getLine(){return line;}

   public Ref reference(int index){
      Ref ref = new Ref(index);
      references.add(ref);
      return ref;
   }
   public void removeReference(Ref reference){
      references.remove(reference);
   }
   public void clearReferences(){references.clear();}
   public int referenceCount(){return references.size();}

   public int getOriginalIndex(int currentIndex){
      int rtrn = currentIndex;
      int idx = drops.size();
      for(int i = idx-1; i>=0; i--){
         Drop d = drops.get(i);
         if(rtrn >= d.getStart()){
            rtrn += d.getEnd();
         }
      }
      return rtrn;
   }

   final void updateReferences(int start,int end){
      drops.add(new Drop(start,end));
      if(!references.isEmpty()){
         for(Ref ref : references){
            int value = ref.get();
            if(value > start){
               int newValue = start;
               if(value > end){
                  newValue =start + (value-end);
               }else{
                  newValue = start;
               }
               ref.set(newValue);
            }
         }
      }
   }

   public abstract void drop(int start,int end);
}
