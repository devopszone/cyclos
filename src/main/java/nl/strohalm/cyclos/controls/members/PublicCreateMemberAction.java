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
package nl.strohalm.cyclos.controls.members;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.controls.BasePublicFormAction;
import nl.strohalm.cyclos.controls.elements.CreateElementAction;
import nl.strohalm.cyclos.entities.access.MemberUser;
import nl.strohalm.cyclos.entities.access.User;
import nl.strohalm.cyclos.entities.customization.fields.CustomField;
import nl.strohalm.cyclos.entities.customization.fields.MemberCustomField;
import nl.strohalm.cyclos.entities.customization.fields.MemberCustomFieldValue;
import nl.strohalm.cyclos.entities.customization.fields.MemberCustomField.Access;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.members.RegisteredMember;
import nl.strohalm.cyclos.entities.settings.AccessSettings;
import nl.strohalm.cyclos.entities.settings.LocalSettings;
import nl.strohalm.cyclos.entities.settings.events.LocalSettingsChangeListener;
import nl.strohalm.cyclos.entities.settings.events.LocalSettingsEvent;
import nl.strohalm.cyclos.exceptions.MailSendingException;
import nl.strohalm.cyclos.services.customization.CustomFieldService;
import nl.strohalm.cyclos.services.elements.ElementService;
import nl.strohalm.cyclos.services.elements.exceptions.UsernameAlreadyInUseException;
import nl.strohalm.cyclos.services.groups.GroupService;
import nl.strohalm.cyclos.servlets.CaptchaServlet;
import nl.strohalm.cyclos.utils.ActionHelper;
import nl.strohalm.cyclos.utils.CustomFieldHelper;
import nl.strohalm.cyclos.utils.SettingsHelper;
import nl.strohalm.cyclos.utils.binding.DataBinder;
import nl.strohalm.cyclos.utils.validation.PasswordsDontMatchError;
import nl.strohalm.cyclos.utils.validation.UsernamesDontMatchError;
import nl.strohalm.cyclos.utils.validation.ValidationError;
import nl.strohalm.cyclos.utils.validation.ValidationException;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import static nl.strohalm.cyclos.controls.members.MemberUtils.setFullNameIfNeeded;

/**
 * Action for a public member registration
 * @author luis
 */
public class PublicCreateMemberAction extends BasePublicFormAction implements LocalSettingsChangeListener {

    private CustomFieldService customFieldService;
    private ElementService     elementService;
    private DataBinder<Member> dataBinder;
    private GroupService       groupService;
    private ReadWriteLock      lock = new ReentrantReadWriteLock();

    public CustomFieldService getCustomFieldService() {
        return customFieldService;
    }

    public DataBinder<Member> getDataBinder() {
        try {
            lock.readLock().lock();
            if (dataBinder == null) {
                final LocalSettings localSettings = SettingsHelper.getLocalSettings(getServlet().getServletContext());
                final AccessSettings accessSettings = SettingsHelper.getAccessSettings(getServlet().getServletContext());
                dataBinder = CreateElementAction.getDataBinder(localSettings, accessSettings, Member.class, MemberUser.class, MemberGroup.class, MemberCustomField.class, MemberCustomFieldValue.class);
            }
            return dataBinder;
        } finally {
            lock.readLock().unlock();
        }
    }

    public ElementService getElementService() {
        return elementService;
    }

    public void onLocalSettingsUpdate(final LocalSettingsEvent event) {
        try {
            lock.writeLock().lock();
            dataBinder = null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Inject
    public void setCustomFieldService(final CustomFieldService customFieldService) {
        this.customFieldService = customFieldService;
    }

    @Inject
    public void setElementService(final ElementService elementService) {
        this.elementService = elementService;
    }

    @Inject
    public void setGroupService(final GroupService groupService) {
        this.groupService = groupService;
    }

    @Override
    protected ActionForward handleDisplay(final ActionMapping mapping, final ActionForm actionForm, final HttpServletRequest request, final HttpServletResponse response) throws Exception {
        // When there's a logged user, redirect to home
        if (request.getSession().getAttribute("loggedUser") != null) {
            try {
                response.sendRedirect("/" + request.getContextPath());
            } catch (final IOException e) {
                // Ignore
            }
            return null;
        }

        return super.handleDisplay(mapping, actionForm, request, response);
    }

    @Override
    protected ActionForward handleSubmit(final ActionMapping mapping, final ActionForm actionForm, final HttpServletRequest request, final HttpServletResponse response) {
        final CreateMemberForm form = (CreateMemberForm) actionForm;
        final HttpSession session = request.getSession();

        // Check the captcha challenge
        if (!CaptchaServlet.checkChallenge(request, form.getCaptcha())) {
            session.setAttribute("forceBack", true);
            throw new ValidationException("createMember.captcha.invalid");
        }

        // Save the member
        final Member member = getDataBinder().readFromString(form.getMember());
        setFullNameIfNeeded(member, request, customFieldService);
        RegisteredMember registeredMember;
        try {
            registeredMember = elementService.publicRegisterMember(member, request.getRemoteAddr());
        } catch (final UsernameAlreadyInUseException e) {
            final ActionForward actionForward = ActionHelper.sendError(mapping, request, response, "createMember.public.alreadyExists");
            session.setAttribute("forceBack", "forceBack");
            return actionForward;
        } catch (final MailSendingException e) {
            return ActionHelper.sendError(mapping, request, response, "createMember.public.errorSendingMail");
        }

        // We will send the flow to the error page not to showing an error, but the created message
        String message;
        if (registeredMember instanceof Member) {
            final Member result = getFetchService().fetch((Member) registeredMember, Element.Relationships.GROUP);
            if (result.isActive()) {
                final User user = result.getUser();
                if (user.getPassword() != null && user.getPasswordDate() == null) {
                    // Member is active and password is sent by mail
                    message = "createMember.public.awaitingPassword";
                } else {
                    // Member is ready
                    message = "createMember.public.validated";
                }
            } else {
                // Member in inactive group. Message is awaiting activation by admin
                message = "createMember.public.awaitingActivation";
            }
        } else {
            // Typed e-mail has to be validated
            message = "createMember.public.awaitingMailValidation";
        }
        session.removeAttribute("forceBack");
        ActionHelper.sendError(mapping, request, response, message, registeredMember.getUsername());
        return mapping.findForward("confirmation");
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void prepareForm(final ActionMapping mapping, final ActionForm actionForm, final HttpServletRequest request, final HttpServletResponse response) {
        final CreateMemberForm form = (CreateMemberForm) actionForm;
        MemberGroup group;
        try {
            group = (MemberGroup) groupService.load(form.getGroupId());
            if (!group.isInitialGroup()) {
                throw new Exception();
            }
        } catch (final Exception e) {
            throw new ValidationException();
        }
        // Get the custom fields
        final List<MemberCustomField> customFields = CustomFieldHelper.onlyForGroup((List<MemberCustomField>) customFieldService.listByNature(CustomField.Nature.MEMBER), group);
        for (final Iterator<MemberCustomField> it = customFields.iterator(); it.hasNext();) {
            final MemberCustomField field = it.next();
            boolean use = true;
            // Only use fields that are visible and editable by members
            final Access visibility = field.getVisibilityAccess();
            if (visibility != null && !visibility.granted(group, true, false, true)) {
                use = false;
            } else {
                // Check if the field can be updated
                final Access update = field.getUpdateAccess();
                if (update != null && !update.granted(group, true, false, true)) {
                    use = false;
                }
            }
            // Remove the field from list if not used
            if (!use) {
                it.remove();
            }
        }
        request.setAttribute("formAction", mapping.getPath());
        request.setAttribute("customFields", customFields);
        request.setAttribute("isPublic", true);
        request.setAttribute("allowSetPassword", true);
        request.setAttribute("group", group);
    }

    @Override
    protected void validateForm(final ActionMapping mapping, final ActionForm actionForm, final HttpServletRequest request, final HttpServletResponse response) throws ValidationException {
        final CreateMemberForm form = (CreateMemberForm) actionForm;

        // Form validation
        final Member member = getDataBinder().readFromString(form.getMember());
        setFullNameIfNeeded(member, request, customFieldService);

        ValidationException exc;
        try {
            elementService.publicValidate(member);
            exc = new ValidationException();
        } catch (final ValidationException e) {
            exc = e;
        }

        final String captcha = form.getCaptcha();
        if (StringUtils.isEmpty(captcha) || !CaptchaServlet.checkChallenge(request, captcha)) {
            exc.addPropertyError("captcha", new ValidationError("createMember.captcha.invalid"));
        }

        //password check
        String password;
        try {
            password = StringUtils.trimToNull(member.getUser().getPassword());
        } catch (final Exception e) {
            password = null;
        }

        final String confirmPassword = StringUtils.trimToNull(form.getConfirmPassword());
        if (password != null && (confirmPassword == null || !ObjectUtils.equals(confirmPassword, member.getUser().getPassword()))) {
            exc.addGeneralError(new PasswordsDontMatchError());
        }

        //Username confirmation check
        String confirmedUsername = StringUtils.trimToNull(form.getConfirmedUsername());
        if(confirmedUsername != null && !StringUtils.equals(confirmedUsername, member.getUsername())){
                exc.addGeneralError(new UsernamesDontMatchError());
        }

        exc.throwIfHasErrors();
    }
}