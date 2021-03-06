/*
   This file is part of Cyclos.

   Cyclos is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 2 of the License, or
   (at your option) any later version.

   Cyclos is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with Cyclos; if not, write to the Free Software
   Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

 */
package nl.strohalm.cyclos.dao.access;

import nl.strohalm.cyclos.dao.BaseDAO;
import nl.strohalm.cyclos.dao.DeletableDAO;
import nl.strohalm.cyclos.dao.InsertableDAO;
import nl.strohalm.cyclos.dao.UpdatableDAO;
import nl.strohalm.cyclos.dao.exceptions.DaoException;
import nl.strohalm.cyclos.dao.exceptions.EntityNotFoundException;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.access.OperatorUser;
import nl.strohalm.cyclos.entities.access.User;
import nl.strohalm.cyclos.entities.members.Member;

/**
 * Data Access Object inteface for User. It does not add any new methods, it does only bind InsertableDAO, UpdatableDAO and DeletableDAO generic type
 * E to User.
 * 
 * @author rafael
 * @author fireblade
 */
public interface UserDAO extends BaseDAO<User>, InsertableDAO<User>, UpdatableDAO<User>, DeletableDAO<User> {

    /**
     * Loads an user by username
     * @throws EntityNotFoundException The given username doesn't exist
     */
    public User load(String username, Relationship... fetch) throws EntityNotFoundException, DaoException;

    /**
     * Loads an operator user
     * @throws EntityNotFoundException The given username doesn't exist for the given member
     */
    public OperatorUser loadOperator(Member member, String username, Relationship... fetch) throws EntityNotFoundException, DaoException;
}
