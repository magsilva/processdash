// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 1999  United States Air Force
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  ken.raisor@hill.af.mil

package pspdash;

public interface ObjectCache {

    /** Return the next available ID for use as a cached object
     * identifier.  This will not return the same number twice.
     */
    int getNextID();

    /** Retrieve a cached object by its ID. */
    public CachedObject getCachedObject(int id, double maxAge);

    /** Delete an object from the cache. */
    public void deleteCachedObject(int id);

    /** Store an object in the cache. */
    public void storeCachedObject(CachedObject obj);

    /** get a list of all the objects in the cache of the specified
     * type. If type is null, all objects are returned. */
    public CachedObject[] getObjects(String type);

    /** get a list of ids for all the objects in the cache of the
     * specified type. If type is null, all objects are returned. */
    public int[] getObjectIDs(String type);

}
