package perf.parse.internal;

/**
 * Created by wreicher
 */
public interface IMatcher {
    public void reset(CharSequence input);
    public boolean find();
    public void region(int start,int end);
    public int start();
    public int end();
    public String group(String name);
}
