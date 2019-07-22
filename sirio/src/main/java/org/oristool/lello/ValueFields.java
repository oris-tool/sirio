/* This program is part of the ORIS Tool.
 * Copyright (C) 2011-2019 The ORIS Authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.oristool.lello;

/**
 * This class is a container of static fields which can be accessed from within
 * the Lello interpreter with their qualified or unqualified Java name.
 *
 * <p>This class is intended for programmers who can modify the Lello source
 * code and that want a fast and easy way to extend the language with new
 * predefined variables which they think will be useful also for someone else
 * later. All fields must be declared static.
 *
 * <p>Programmers which can not modify the Lello source, or programmers that
 * have a very specific need and do not want to add clutter to this class, can
 * declare static fields anywhere else in their source code; as long as Lello is
 * given the qualified name it will be able to access them.
 */
public class ValueFields {

    /**
     * The current version name of Lello.
     */
    public static final String VERSION_STRING = "Lello 1.0";
}
