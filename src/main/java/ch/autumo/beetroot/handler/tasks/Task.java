/**
 * Generated by PLANT - beetRoot CRUD Generator.
 */
package ch.autumo.beetroot.handler.tasks;

import ch.autumo.beetroot.Model;
import ch.autumo.beetroot.annotations.Column;
import ch.autumo.beetroot.annotations.Nullable;
import ch.autumo.beetroot.annotations.Unique;

/**
 * Task. 
 */
public class Task extends Model {

    private static final long serialVersionUID = 1L;
    
	@Column(name = "monthofyear")
	private String monthofyear;

	public String getMonthofyear() {
        return monthofyear;
    }

    public void setMonthofyear(String monthofyear) {
        this.monthofyear = monthofyear;
    }

	@Nullable
	@Column(name = "created")
    private java.sql.Timestamp created;

    public java.sql.Timestamp getCreated() {
        return created;
    }

    public void setCreated(java.sql.Timestamp created) {
        this.created = created;
    }

	@Column(name = "active")
    private boolean active;

    public boolean getActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

	@Column(name = "minute")
    private String minute;

    public String getMinute() {
        return minute;
    }

    public void setMinute(String minute) {
        this.minute = minute;
    }

	@Column(name = "dayofmonth")
    private String dayofmonth;

    public String getDayofmonth() {
        return dayofmonth;
    }

    public void setDayofmonth(String dayofmonth) {
        this.dayofmonth = dayofmonth;
    }

	@Column(name = "path")
    private String path;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

	@Column(name = "hour")
    private String hour;

    public String getHour() {
        return hour;
    }

    public void setHour(String hour) {
        this.hour = hour;
    }

	@Unique
	@Column(name = "name")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

	@Nullable
	@Column(name = "guid")
    private String guid;

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

	@Column(name = "dayofweek")
    private String dayofweek;

    public String getDayofweek() {
        return dayofweek;
    }

    public void setDayofweek(String dayofweek) {
        this.dayofweek = dayofweek;
    }

	@Column(name = "laststatus")
    private boolean laststatus;

    public boolean getLaststatus() {
        return laststatus;
    }

    public void setLaststatus(boolean laststatus) {
        this.laststatus = laststatus;
    }

	@Nullable
	@Column(name = "modified")
    private java.sql.Timestamp modified;

    public java.sql.Timestamp getModified() {
        return modified;
    }

    public void setModified(java.sql.Timestamp modified) {
        this.modified = modified;
    }

	@Nullable
	@Column(name = "lastexecuted")
    private java.sql.Timestamp lastexecuted;

    public java.sql.Timestamp getLastexecuted() {
        return lastexecuted;
    }

    public void setLastexecuted(java.sql.Timestamp lastexecuted) {
        this.lastexecuted = lastexecuted;
    }

	@Override
	public String getDisplayField() {
		return "name";
	}
    
	@Override
	public Class<?> modelClass() {
		return Task.class;
	}

	
}
