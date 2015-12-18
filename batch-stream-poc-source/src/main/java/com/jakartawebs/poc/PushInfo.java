package com.jakartawebs.poc;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;
import javax.validation.constraints.NotNull;

@Entity
@Table(name="push_info")
@SuppressWarnings("serial")
public class PushInfo implements Serializable {
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	@Column(name="id")
	private Long id;
	
	/**
	 * Data start timestamp.
	 */
	@NotNull
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="start_timestamp")
	private Date startTimestamp;
	
	/**
	 * Data end timestamp.
	 */
	@NotNull
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="end_timestamp")
	private Date endTimestamp;

	@Column(name="is_completed")
	private boolean completed;
	
	@Column(name="is_last_execution")
	private boolean lastExecution;
	
	@Version
	@Column(name="record_version")
	private Integer version;
	
	public PushInfo() {}
	public PushInfo(Date startTimestamp, Date endTimestamp) {
		this.startTimestamp = startTimestamp;
		this.endTimestamp = endTimestamp;
	}
	
	public Long getId() {
		return id;
	}
	
	public Date getStartTimestamp() {
		return startTimestamp;
	}
	public void setStartTimestamp(Date startTimestamp) {
		this.startTimestamp = startTimestamp;
	}
	
	public Date getEndTimestamp() {
		return endTimestamp;
	}
	public void setEndTimestamp(Date endTimestamp) {
		this.endTimestamp = endTimestamp;
	}
	
	public boolean isCompleted() {
		return completed;
	}
	public void setCompleted(boolean completed) {
		this.completed = completed;
	}
	
	public boolean isLastExecution() {
		return lastExecution;
	}
	public void setLastExecution(boolean lastExecution) {
		this.lastExecution = lastExecution;
	}
	
	public Integer getVersion() {
		return version;
	}
}
