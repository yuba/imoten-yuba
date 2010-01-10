package immf;

public class Emoji {
	private char c;
	private String label;
	private String googleImage;
	
	public Emoji(char c, String label,String googleImage){
		this.c = c;
		this.label = label;
		this.googleImage = googleImage;
	}
	
	public char getC() {
		return c;
	}
	public String getLabel() {
		return label;
	}
	public String getgoogleImage() {
		return googleImage;
	}
	
	
}
