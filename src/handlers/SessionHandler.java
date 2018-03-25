package handlers;

import catalogs.CatalogUser;
import domain.*;
import exceptions.WrongUserPasswordException;

public class SessionHandler extends GodHandler implements handlers.Interface.ISessionHandler {

    private static CatalogUser catUser;

    public SessionHandler() {
        catUser = CatalogUser.getCatalog();
    }

    @Override
    public boolean startSession(String userID, String pass) throws WrongUserPasswordException {

        User u = catUser.getUser(userID);
        if (u == null) {
            u = new User(userID, pass);
            catUser.addUser(u);
            setCurrUser(u);
            return false;
        }

        if (u.isSamePwd(pass)) {
            setCurrUser(u);
            return true;
        }

        throw new WrongUserPasswordException();

    }

}
