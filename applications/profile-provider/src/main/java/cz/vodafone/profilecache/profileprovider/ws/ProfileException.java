package cz.vodafone.profilecache.profileprovider.ws;

import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class ProfileException extends Exception implements Serializable{

	@XmlElement(required = true)
	private int reasonCode;
	
	public ProfileException() {		
	}

	public ProfileException(int reasonCode, String message) {
		super(message);
		this.reasonCode = reasonCode;
	}

	public ProfileException(Throwable cause) {
		super(cause);		
	}

	public ProfileException(String message, Throwable cause) {
        super(message, cause);
    }
	
	public ProfileException(int reasonCode, String message, Throwable cause) {
		super(message, cause);
		this.reasonCode = reasonCode;
	}

	public int getReasonCode() {
		return reasonCode;
	}

	public void setReasonCode(int reasonCode) {
		this.reasonCode = reasonCode;
	}

}
