package pt.up.fe.comp.jmm.ast;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import pt.up.fe.comp.jmm.JmmNode;

public class JmmNodeImpl implements JmmNode {

    protected String kind;
    protected Map<String, String> attributes;
    protected List<JmmNode> children;
    private JmmNode parent;
    

    public JmmNodeImpl(String kind) {
        this.kind = kind;
        this.children = new ArrayList<>();
        this.attributes = new LinkedHashMap<>();
    }

    public JmmNode getParent() {
        return this.parent;
    }

    @Override
    public List<JmmNode> getChildren() {
        return this.children;
    }

    @Override
    public String getKind() {
        return this.kind;
    }

    @Override
    public List<String> getAttributes() {
        return new ArrayList<>(this.attributes.keySet());
    }

    @Override
    public String get(String attribute) {

        return this.attributes.get(attribute);
    }

    @Override
    public int getNumChildren() {
        return this.children.size();
    }



    @Override
    public void put(String attribute, String value) {
        this.attributes.put(attribute, value);
    }

    @Override
    public void add(JmmNode child) {
        if(!(child instanceof JmmNodeImpl)){
            throw new RuntimeException(getClass().getName()+" can only have children of his class ("+getClass().getName()+").");
        }
        add((JmmNodeImpl)child);
    }

    public void add(JmmNodeImpl child) {
        children.add(child);
    }



    @Override
    public void add(JmmNode child, int index) {
        if(!(child instanceof JmmNodeImpl)){
            throw new RuntimeException(getClass().getName()+" can only have children of his class ("+getClass().getName()+").");
        }
        add((JmmNodeImpl)child,index);
    }

    public void add(JmmNodeImpl child, int index) {
        this.children.add(index, child);
    }

    /**
     * Convert the string into a JmmNode instance
     * 
     * @param <N>
     * @param source
     * @param nodeClass
     * @return
     */
    public static JmmNodeImpl fromJson(String source) {

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(JmmNode.class, new JmmDeserializer())
                .registerTypeAdapter(JmmNodeImpl.class, new JmmDeserializer())
                .create();
        return gson.fromJson(source, JmmNodeImpl.class);
    }


    public void setParent(JmmNodeImpl parent) {
        this.parent = parent;
    }
}
