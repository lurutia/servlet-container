import com.study.scbook.ch05.Callee;

public class CalleeImpl implements Callee {
    private String name = "orgName";
    public void setName(String name) {
        this.name = name;
    }
    public String getName() {
        return this.name;
    }
}
