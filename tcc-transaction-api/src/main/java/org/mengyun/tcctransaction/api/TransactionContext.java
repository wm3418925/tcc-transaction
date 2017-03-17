package org.mengyun.tcctransaction.api;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by changmingxie on 10/30/15.
 */
public class TransactionContext implements Serializable {

    private static final long serialVersionUID = -8199390103169700387L;
    private TransactionXid xid;

    private int status;

    private Map<String, Object> attachments = new HashMap<String, Object>();

    public TransactionContext() {

    }

    public TransactionContext(TransactionXid xid, int status) {
        this.xid = xid;
        this.status = status;
    }


    public Object get(String key) {
        return attachments.get(key);
    }
    public Object put(String key, Object value) {
        return attachments.put(key, value);
    }
    public Object remove(String key) {
        return attachments.remove(key);
    }
    public boolean containsKey(String key) {
        return attachments.containsKey(key);
    }
    public boolean containsValue(Object value) {
        return attachments.containsValue(value);
    }
    public int size() {
        return attachments.size();
    }
    public Set<Map.Entry<String,Object>> entrySet() {
        return attachments.entrySet();
    }



    public void setXid(TransactionXid xid) {
        this.xid = xid;
    }

    public TransactionXid getXid() {
        return xid.clone();
    }

    @Deprecated
    public void setAttachments(Map<String, Object> attachments) {
        this.attachments = attachments;
    }

    public Map<String, Object> getAttachments() {
        return attachments;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }


}
