/*
 * Entagged Audio Tag library
 * Copyright (c) 2003-2005 Raphaël Slinckx <raphael@slinckx.net>
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *  
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.jaudiotagger.tag;

import org.jaudiotagger.tag.datatype.Artwork;

import java.util.Iterator;
import java.util.List;

/**
 * This interface represents the basic data structure for the default
 * audiolibrary functionality.<br>
 * <p/>
 * Some audio file tagging systems allow to specify multiple values for one type
 * of information. The artist for example. Some songs may be a cooperation of
 * two or more artists. Sometimes a tagging user wants to specify them in the
 * tag without making one long text string.<br>
 * For that kind of fields, the <b>commonly</b> used fields have adequate
 * methods for adding values. But it is possible the underlying implementation
 * does not support that kind of storing multiple values.<br>
 * <br>
 * <b>Code Examples:</b><br>
 * <p/>
 * <pre>
 * <code>
 * AudioFile file = AudioFileIO.read(new File(&quot;C:\\test.mp3&quot;));
 * <p/>
 * Tag tag = file.getTag();
 * </code>
 * </pre>
 *
 * @author Raphael Slinckx
 */
public interface Tag {
    /**
     * Create the field based on the generic key and set it in the tag
     *
     * @param genericKey
     * @param value
     * @throws KeyNotFoundException
     * @throws FieldDataInvalidException
     */
    public void setField(FieldKey genericKey, String value) throws KeyNotFoundException, FieldDataInvalidException;

    /**
     * Create the field based on the generic key and add it to the tag
     *
     * @param genericKey
     * @param value
     * @throws KeyNotFoundException
     * @throws FieldDataInvalidException
     */
    public void addField(FieldKey genericKey, String value) throws KeyNotFoundException, FieldDataInvalidException;

    /**
     * Delete any fields with this key
     *
     * @param fieldKey
     * @throws KeyNotFoundException
     */
    public void deleteField(FieldKey fieldKey) throws KeyNotFoundException;

    /**
     * Returns a {@linkplain List list} of {@link TagField} objects whose &quot;{@linkplain TagField#getId() id}&quot;
     * is the specified one.<br>
     *
     * @param id The field id.
     * @return A list of {@link TagField} objects with the given &quot;id&quot;.
     */
    public List<TagField> get(String id);


    /**
     * Retrieve String value of the first value that exists for this format specific key
     * <p/>
     * <p>Can be used to retrieve fields with any identifier, useful if the identifier is not within {@link FieldKey}
     *
     * @param id
     * @return
     */
    public String getFirst(String id);

    /**
     * Retrieve String value of the first tagfield that exists for this generic key
     *
     * @param id
     * @return String value or empty string
     * @throws KeyNotFoundException
     */
    public String getFirst(FieldKey id) throws KeyNotFoundException;

    /**
     * Retrieve the first field that exists for this format specific key
     * <p/>
     * <p>Can be used to retrieve fields with any identifier, useful if the identifier is not within {@link FieldKey}
     *
     * @param id audio specific key
     * @return tag field or null if doesnt exist
     */
    public TagField getFirstField(String id);

    /**
     * @param id
     * @return the first field that matches this generic key
     */
    public TagField getFirstField(FieldKey id);


    /**
     * Returns <code>true</code>, if at least one of the contained
     * {@linkplain TagField fields} is a common field ({@link TagField#isCommon()}).
     *
     * @return <code>true</code> if a {@linkplain TagField#isCommon() common}
     *         field is present.
     */
    public boolean hasCommonFields();

    /**
     * Determines whether the tag has at least one field with the specified
     * &quot;id&quot;.
     *
     * @param id The field id to look for.
     * @return <code>true</code> if tag contains a {@link TagField} with the
     *         given {@linkplain TagField#getId() id}.
     */
    public boolean hasField(String id);

    /**
     * Determines whether the tag has no fields specified.<br>
     *
     * @return <code>true</code> if tag contains no field.
     */
    public boolean isEmpty();


    //TODO, do we need this
    public String toString();

    /**
     * Returns a {@linkplain List list} of {@link TagField} objects whose &quot;{@linkplain TagField#getId() id}&quot;
     * is the specified one.<br>
     *
     * @param id The field id.
     * @return A list of {@link TagField} objects with the given &quot;id&quot;.
     * @throws KeyNotFoundException
     */
    public List<TagField> getFields(FieldKey id) throws KeyNotFoundException;


    /**
     * Iterator over all the fields within the tag, handle multiple fields with the same id
     *
     * @return iterator over whole list
     */
    public Iterator<TagField> getFields();

    /**
     * Return the number of fields
     * <p/>
     * <p>Fields with the same identifiers are counted seperately
     * i.e two title fields would contribute two to the count
     *
     * @return total number of fields
     */
    public int getFieldCount();

    //TODO is this a special field?
    public boolean setEncoding(String enc) throws FieldDataInvalidException;


    /**
     * @return a list of all artwork in this file using the format indepedent Artwork class
     */
    public List<Artwork> getArtworkList();

    /**
     * @return first artwork or null if none exist
     */
    public Artwork getFirstArtwork();

    /**
     * Delete any instance of tag fields used to store artwork
     *
     * <p>We need this additional deleteField method because in some formats artwork can be stored
     * in multiple fields
     *
     * @throws KeyNotFoundException
     */
    public void deleteArtworkField() throws KeyNotFoundException;


    /**
     * Create artwork field based on the data in artwork
     *
     * @param artwork
     * @return suitable tagfield for this format that represents the artwork data
     * @throws FieldDataInvalidException
     */
    public TagField createField(Artwork artwork) throws FieldDataInvalidException;

    /**
     * Create artwork field based on the data in artwork and then set it in the tag itself
     * <p/>
     *
     * @param artwork
     * @throws FieldDataInvalidException
     */
    public void setField(Artwork artwork) throws FieldDataInvalidException;

    /**
     * Create artwork field based on the data in artwork and then add it to the tag itself
     * <p/>
     *
     * @param artwork
     * @throws FieldDataInvalidException
     */
    public void addField(Artwork artwork) throws FieldDataInvalidException;

    /**
     * Sets a field in the structure, used internally by the library<br>
     * <p/>
     * <p>It is not recommended to use this method for normal use of the
     * audiolibrary. The developer will circumvent the underlying
     * implementation. For example, if one adds a field with the field id
     * &quot;TALB&quot; for an mp3 file, and the given {@link TagField}
     * implementation does not return a text field compliant data with
     * {@link TagField#getRawContent()} other software and the audio library
     * won't read the file correctly, if they do read it at all. <br>
     * So for short:<br>
     * <uil>
     * <li>The field is stored without validation</li>
     * <li>No conversion of data is perfomed</li>
     * </ul>
     *
     * @param field The field to add.
     * @throws FieldDataInvalidException
     */
    public void setField(TagField field) throws FieldDataInvalidException;

    /**
     * Adds a field to the structure, used internally by the library<br>
     * <p/>
     * <p>It is not recommended to use this method for normal use of the
     * audiolibrary. The developer will circumvent the underlying
     * implementation. For example, if one adds a field with the field id
     * &quot;TALB&quot; for an mp3 file, and the given {@link TagField}
     * implementation does not return a text field compliant data with
     * {@link TagField#getRawContent()} other software and the audio library
     * won't read the file correctly, if they do read it at all. <br>
     * So for short:<br>
     * <uil>
     * <li>The field is stored without validation</li>
     * <li>No conversion of data is perfomed</li>
     * </ul>
     *
     * @param field The field to add.
     * @throws FieldDataInvalidException
     */
    public void addField(TagField field) throws FieldDataInvalidException;

    /**
     * Create a new field based on generic key, used internally by the library
     * <p/>
     * <p>Only textual data supported at the moment. The genericKey will be mapped
     * to the correct implementation key and return a TagField.
     * <p/>
     * It is not recommended to use this method for normal use of the
     * audiolibrary, because this field is actually added to the structure
     *
     * @param genericKey is the generic key
     * @param value      to store
     * @return
     * @throws KeyNotFoundException
     * @throws FieldDataInvalidException
     */
    public TagField createField(FieldKey genericKey, String value) throws KeyNotFoundException, FieldDataInvalidException;


}