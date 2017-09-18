package gov.noaa.pmel.dashboard.shared;

import java.io.Serializable;
import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;

public class FileInfo implements Serializable, IsSerializable {
	
	private static final long serialVersionUID = 6902786970204160458L;
	
	private String fileName;
	private Date fileModTime;
	private Date fileCreateTime;
	private long fileSize;
	
	@SuppressWarnings("unused")
	private FileInfo() {
	}
	
	public FileInfo(String fileName) {
		super();
		this.fileName = fileName;
	}

	public FileInfo(String fileName, Date fileModTime, Date fileCreateTime, long fileSize) {
		this(fileName);
		this.fileModTime = fileModTime;
		this.fileCreateTime = fileCreateTime;
		this.fileSize = fileSize;
	}

	public Date getFileModTime() {
		return fileModTime;
	}

	public void setFileModTime(Date fileModTime) {
		this.fileModTime = fileModTime;
	}

	public Date getFileCreateTime() {
		return fileCreateTime;
	}

	public void setFileCreateTime(Date fileCreateTime) {
		this.fileCreateTime = fileCreateTime;
	}

	public long getFileSize() {
		return fileSize;
	}

	public void setFileSize(long fileSize) {
		this.fileSize = fileSize;
	}

	public String getFileName() {
		return fileName;
	}
	
	
}
