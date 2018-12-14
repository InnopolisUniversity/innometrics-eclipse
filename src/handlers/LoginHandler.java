package handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import server.Server;

public class LoginHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		new Thread(this::openLoginWindow).start();
		return null;
	}
	
	public void openLoginWindow() {
		Server server = Server.getInstance();
		server.askedForCredentials = false;
		server.getCredentials("Please, login to the logger");
	}

}
