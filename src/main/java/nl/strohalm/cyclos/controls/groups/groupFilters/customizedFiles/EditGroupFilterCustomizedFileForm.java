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
package nl.strohalm.cyclos.controls.groups.groupFilters.customizedFiles;

import java.util.Map;

import nl.strohalm.cyclos.controls.BaseBindingForm;

/**
 * Form used to edit a customized file for a group filter
 * @author luis
 */
public class EditGroupFilterCustomizedFileForm extends BaseBindingForm {
    private static final long serialVersionUID = -8978223401408817974L;
    private long              groupFilterId;
    private long              fileId;

    public Map<String, Object> getFile() {
        return values;
    }

    public Object getFile(final String key) {
        return values.get(key);
    }

    public long getFileId() {
        return fileId;
    }

    public long getGroupFilterId() {
        return groupFilterId;
    }

    public void setFile(final Map<String, Object> file) {
        values = file;
    }

    public void setFile(final String key, final Object value) {
        values.put(key, value);
    }

    public void setFileId(final long fileId) {
        this.fileId = fileId;
    }

    public void setGroupFilterId(final long groupFilterId) {
        this.groupFilterId = groupFilterId;
    }

}
