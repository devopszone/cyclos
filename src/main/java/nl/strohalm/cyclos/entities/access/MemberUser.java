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
package nl.strohalm.cyclos.entities.access;

import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.settings.LocalSettings;

import java.util.Map;

/**
 * A user for regular members or brokers
 * @author luis
 */
public class MemberUser extends User {

    private static final long serialVersionUID = -524317381029059040L;

    private String            pin;
    private transient boolean passwordGenerated;
    private String            loginPassword;

    public Member getMember() {
        return (Member) super.getElement();
    }

    public String getPin() {
        return pin;
    }

    public boolean isPasswordGenerated() {
        return passwordGenerated;
    }

    public void setPasswordGenerated(final boolean passwordGenerated) {
        this.passwordGenerated = passwordGenerated;
    }

    public void setPin(final String pin) {
        this.pin = pin;
    }

    public String getLoginPassword() {
        return loginPassword;
    }

    public void setLoginPassword(String loginPassword) {
        this.loginPassword = loginPassword;
    }

    @Override
    protected void appendVariableValues(Map<String, Object> variables, LocalSettings localSettings) {
        super.appendVariableValues(variables, localSettings);
        variables.put("password", getLoginPassword());
    }
}
