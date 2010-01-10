package immf;

public class LoginException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2073855443374349319L;
	public LoginException(String msg){
		super(msg);
	}
	public LoginException(String msg, Throwable cause){
		super(msg,cause);
	}
	public LoginException(Throwable cause){
		super(cause);
	}
}
