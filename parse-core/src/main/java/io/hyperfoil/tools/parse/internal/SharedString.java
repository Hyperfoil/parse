package io.hyperfoil.tools.parse.internal;

import java.util.BitSet;


/**
 */
public class SharedString extends DropString implements CharSequence {

   private BitSet bits;
   private int start;
   private int stop;
   private SharedString parent;


   public SharedString(String line){
      this(line,new BitSet(line.length()),0,line.length(),null,true);
   }
   public SharedString(String line,int start,int stop,SharedString parent){
      this(line,new BitSet(stop-start),start,stop,parent,true);
   }
   private SharedString(String line,BitSet bits,int start,int stop,SharedString parent,boolean flip){
      super(line);
      this.line = line;
      this.bits = bits;
      this.start = start;
      this.stop = stop;
      this.parent = parent;
      if(flip){//we own the bits, flip them
         bits.flip(start,stop);
      }
   }
   public String getLine(){return line;}
   public int getAbsoluteIndex(int index){
      if(index > bits.cardinality()){
         throw new IndexOutOfBoundsException(index + " > " + bits.cardinality());
      }
      int targetIndex = -1;
      int sum = -1;
      while(sum < index && ++targetIndex < line.length() ){
         if(bits.get(targetIndex)){
            sum++;
         }
      }
      return targetIndex;
   }
   public int getAbsoluteIndex2(int relativeIndex){
      int currentIndex = 0;
      while(relativeIndex>=0 && currentIndex < bits.length()){
         if(bits.get(currentIndex)){
            relativeIndex--;
         }
         currentIndex++;
      }
      return --currentIndex + start;
   }
   public int getRelativeIndex(int absoluteIndex){
      if(absoluteIndex<0 || absoluteIndex >= line.length()){
         throw new IndexOutOfBoundsException(absoluteIndex+" out of range [0,"+line.length()+")");
      }
      return bits.get(0,absoluteIndex).cardinality();
   }
   @Override
   public void drop(int start,int end){
      if(start>end || start<0 || end > line.length()){
         throw new IllegalArgumentException("Invalid drop range. [length="+line.length()+" start="+start+" end="+end+"] line="+this.toString());
      }
      int startIndex = getAbsoluteIndex(start);//TODO change to getAbsoluteIndex
      int iterIndex = startIndex;
      int sum = end - start;
      while(iterIndex < line.length() && sum > 0){
         if(bits.get(iterIndex)){
            sum--;
            bits.clear(iterIndex);
         }
         iterIndex++;
      }
      if(parent!=null){
         parent.childDropRange(startIndex,iterIndex);
      }
      updateReferences(start,end);
   }
   private void childDropRange(int start,int end){
      int relativeStart = getRelativeIndex(start);
      int relativeEnd = getRelativeIndex(end);
      this.drop(relativeStart,relativeEnd);
   }
   public String debug(){
      StringBuffer chars = new StringBuffer();
      StringBuffer bits = new StringBuffer();
      StringBuffer indx = new StringBuffer();
      for(int i=0; i<stop-start; i++){
         chars.append(String.format("%3s",""+line.charAt(start+i)));
         bits.append(String.format("%3s",(this.bits.get(i)?" ":"x")));
         indx.append(String.format("%3d",i));
      }
      return indx.toString()+"\n"+chars.toString()+"\n"+bits.toString();
   }

   @Override
   public int length() {
      return bits.cardinality();
   }

   @Override
   public char charAt(int index) {
      return line.charAt(getAbsoluteIndex(index));
   }

   @Override
   public SharedString subSequence(int start, int end) {
      StringBuffer stringBuffer = new StringBuffer();
      int startIndex = getAbsoluteIndex(start);
      int endIndex = getAbsoluteIndex(end);//+1?

      return new SharedString(getLine(),startIndex,endIndex,this);
//      int sum = end-start;
//      while(sum>0 && startIndex<line.length()){
//         if(bits.get(startIndex)){
//            stringBuffer.append(line.charAt(startIndex));
//            sum--;
//         }
//         startIndex++;
//      }
//      return stringBuffer.toString();
   }

   @Override
   public String toString(){
      StringBuffer stringBuffer = new StringBuffer();
      int startIndex = 0;
      int endIndex = line.length();//+1?
      int sum = length();
      while(sum>0 && startIndex<line.length()){
         if(bits.get(startIndex)){
            stringBuffer.append(line.charAt(startIndex));
            sum--;
         }
         startIndex++;
      }
      return stringBuffer.toString();
   }
}
