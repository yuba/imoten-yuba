package immf;

import java.io.IOException;

public interface MyWiserMailListener {
	void receiveMail(MyWiserMessage msg) throws IOException;
}
