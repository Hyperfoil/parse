package perf.parse.internal;

import java.util.Set;

/**
 * Created by wreicher
 */
public interface IMatcher {
    public Set<String> groups();
    public void reset(CharSequence input);
    public boolean find();
    public boolean find(CharSequence input,int start, int end);
    public void region(int start,int end);
    public int start();
    public int end();
    public String group(String name);
}
