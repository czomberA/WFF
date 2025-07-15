package pl.edu.pja.dziabor.GUI;


import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractListModel;


public class ObjectsModel<T> extends AbstractListModel<T> {
    private static final long serialVersionUID = -7858066195290617794L;
    ArrayList<T> objects;

    public ObjectsModel(List<T> objs) {
        objects = new ArrayList<T>(objs);
    }

    public ObjectsModel() {
        // TODO Auto-generated constructor stub
        objects = new ArrayList<T>();
    }

    @Override
    public T getElementAt(int index) {
        return objects.get(index);
    }

    @Override
    public int getSize() {
        return objects.size();
    }

    public void addObject(T newObj) {
        objects.add(newObj);

        // Inform the model about the change
        fireContentsChanged(this, getSize() - 1, getSize() - 1);
    }

    public void removeObject(T obj) {
        objects.remove(obj);
        // Inform the model about the change
        fireContentsChanged(this, getSize() - 1, getSize() - 1);
    }

    public List<T> getObjects() {
        return objects;
    }

    public void clear() {
        objects.clear();
        // TODO Auto-generated method stub

    }

}