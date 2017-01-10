package org.jaudiotagger.tag.vorbiscomment;

import org.jaudiotagger.tag.reference.Tagger;
import org.jaudiotagger.tag.mp4.field.Mp4FieldType;

import java.util.List;
import java.util.EnumSet;

/**
 * Vorbis Comment Field Names
 * <p/>
 * <p/>
 * <p/>
 * This partial list is derived fom the following sources:
 * <ul>
 * <li>http://xiph.org/vorbis/doc/v-comment.html</li>
 * <li>http://wiki.musicbrainz.org/PicardQt/TagMapping</li>
 * <li>http://reactor-core.org/ogg-tagging.html</li>
 * </ul>
 */
public enum VorbisCommentFieldKey
{
    ARTIST("ARTIST", EnumSet.of(Tagger.XIPH,Tagger.PICARD,Tagger.JAIKOZ)),
    VERSION("VERSION", EnumSet.of(Tagger.XIPH)),// The version field may be used to differentiate multiple versions of the same track title in a single collection. (e.g. remix info)
    ALBUM("ALBUM", EnumSet.of(Tagger.XIPH,Tagger.PICARD,Tagger.JAIKOZ)),
    DESCRIPTION("DESCRIPTION",EnumSet.of(Tagger.XIPH)),
    GENRE("GENRE",EnumSet.of(Tagger.XIPH,Tagger.PICARD,Tagger.JAIKOZ)),
    TITLE("TITLE", EnumSet.of(Tagger.XIPH,Tagger.PICARD,Tagger.JAIKOZ)),
    TRACKNUMBER("TRACKNUMBER",EnumSet.of(Tagger.XIPH,Tagger.PICARD,Tagger.JAIKOZ)),
    DATE("DATE",EnumSet.of(Tagger.XIPH,Tagger.PICARD,Tagger.JAIKOZ)),
    COPYRIGHT("COPYRIGHT",EnumSet.of(Tagger.XIPH,Tagger.PICARD,Tagger.JAIKOZ)),
    LICENSE("LICENSE",EnumSet.of(Tagger.XIPH)),
    LOCATION("LOCATION",EnumSet.of(Tagger.XIPH)),
    CONTACT("CONTACT",EnumSet.of(Tagger.XIPH)),
    COMMENT("COMMENT",EnumSet.of(Tagger.PICARD)),
    ALBUMARTIST("ALBUMARTIST",EnumSet.of(Tagger.PICARD,Tagger.JAIKOZ)),
    COMPOSER("COMPOSER",EnumSet.of(Tagger.PICARD,Tagger.JAIKOZ)),
    GROUPING("GROUPING",EnumSet.of(Tagger.PICARD,Tagger.JAIKOZ)),
    DISCNUMBER("DISCNUMBER",EnumSet.of(Tagger.PICARD,Tagger.JAIKOZ)),
    BPM("BPM",EnumSet.of(Tagger.PICARD,Tagger.JAIKOZ)),
    MUSICBRAINZ_ARTISTID("MUSICBRAINZ_ARTISTID",EnumSet.of(Tagger.PICARD,Tagger.JAIKOZ)),
    MUSICBRAINZ_ALBUMID("MUSICBRAINZ_ALBUMID",EnumSet.of(Tagger.PICARD,Tagger.JAIKOZ)),
    MUSICBRAINZ_ALBUMARTISTID("MUSICBRAINZ_ALBUMARTISTID",EnumSet.of(Tagger.PICARD,Tagger.JAIKOZ)),
    MUSICBRAINZ_TRACKID("MUSICBRAINZ_TRACKID",EnumSet.of(Tagger.PICARD,Tagger.JAIKOZ)),
    MUSICBRAINZ_DISCID("MUSICBRAINZ_DISCID",EnumSet.of(Tagger.PICARD,Tagger.JAIKOZ)),
    MUSICIP_PUID("MUSICIP_PUID",EnumSet.of(Tagger.PICARD,Tagger.JAIKOZ)),
    ASIN("ASIN",EnumSet.of(Tagger.PICARD,Tagger.JAIKOZ)),
    MUSICBRAINZ_ALBUMSTATUS("MUSICBRAINZ_ALBUMSTATUS",EnumSet.of(Tagger.PICARD,Tagger.JAIKOZ)),
    MUSICBRAINZ_ALBUMTYPE("MUSICBRAINZ_ALBUMTYPE",EnumSet.of(Tagger.PICARD,Tagger.JAIKOZ)),
    RELEASECOUNTRY("RELEASECOUNTRY",EnumSet.of(Tagger.PICARD,Tagger.JAIKOZ)),
    LYRICS("LYRICS",EnumSet.of(Tagger.PICARD,Tagger.JAIKOZ)),
    COMPILATION("COMPILATION",EnumSet.of(Tagger.PICARD,Tagger.JAIKOZ)),
    ARTISTSORT("ARTISTSORT",EnumSet.of(Tagger.PICARD,Tagger.JAIKOZ)),
    ALBUMARTISTSORT("ALBUMARTISTSORT",EnumSet.of(Tagger.PICARD,Tagger.JAIKOZ)),
    ALBUMSORT("ALBUMSORT",EnumSet.of(Tagger.PICARD,Tagger.JAIKOZ)),
    TITLESORT("TITLESORT",EnumSet.of(Tagger.PICARD,Tagger.JAIKOZ)),
    COMPOSERSORT("COMPOSERSORT",EnumSet.of(Tagger.JAIKOZ)),
    COVERARTMIME("COVERARTMIME",EnumSet.of(Tagger.JAIKOZ)),
    COVERART("COVERART",EnumSet.of(Tagger.JAIKOZ)),
    VENDOR("VENDOR"),
    ISRC("ISRC",EnumSet.of(Tagger.XIPH,Tagger.PICARD,Tagger.JAIKOZ)),
    BARCODE("BARCODE",EnumSet.of(Tagger.JAIKOZ)),
    CATALOGNUMBER("CATALOGNUMBER",EnumSet.of(Tagger.PICARD,Tagger.JAIKOZ)),
    LABEL("LABEL",EnumSet.of(Tagger.PICARD,Tagger.JAIKOZ)),
    ORGANIZATION("ORGANIZATION",EnumSet.of(Tagger.XIPH)),  //   Name of the organization producing the track (i.e. the 'record label')
    LYRICIST("LYRICIST",EnumSet.of(Tagger.PICARD,Tagger.JAIKOZ)),
    CONDUCTOR("CONDUCTOR",EnumSet.of(Tagger.PICARD,Tagger.JAIKOZ)),
    PERFORMER("PERFORMER",EnumSet.of(Tagger.XIPH,Tagger.PICARD)),
    REMIXER("REMIXER",EnumSet.of(Tagger.PICARD,Tagger.JAIKOZ)),
    MOOD("MOOD",EnumSet.of(Tagger.PICARD,Tagger.JAIKOZ)),
    MEDIA("MEDIA",EnumSet.of(Tagger.PICARD,Tagger.JAIKOZ)),
    URL_OFFICIAL_RELEASE_SITE("URL_OFFICIAL_RELEASE_SITE",EnumSet.of(Tagger.JAIKOZ)),
    URL_DISCOGS_RELEASE_SITE("URL_DISCOGS_RELEASE_SITE",EnumSet.of(Tagger.JAIKOZ)),
    URL_WIKIPEDIA_RELEASE_SITE("URL_WIKIPEDIA_RELEASE_SITE",EnumSet.of(Tagger.JAIKOZ)),
    URL_OFFICIAL_ARTIST_SITE("URL_OFFICIAL_ARTIST_SITE",EnumSet.of(Tagger.JAIKOZ)),
    URL_DISCOGS_ARTIST_SITE("URL_DISCOGS_ARTIST_SITE",EnumSet.of(Tagger.JAIKOZ)),
    URL_WIKIPEDIA_ARTIST_SITE("URL_WIKIPEDIA_ARTIST_SITE",EnumSet.of(Tagger.JAIKOZ)),
    KEY("KEY"),
    LANGUAGE("LANGUAGE"),
    URL_LYRICS_SITE("URL_LYRICS_SITE",EnumSet.of(Tagger.JAIKOZ)),
    TRACKTOTAL("TRACKTOTAL",EnumSet.of(Tagger.XIPH,Tagger.PICARD)),
    DISCTOTAL("DISCTOTAL",EnumSet.of(Tagger.XIPH,Tagger.PICARD)),
    ENCODEDBY("ENCODEDBY",EnumSet.of(Tagger.PICARD)),
    ENCODER("ENCODER"),
    METADATA_BLOCK_PICTURE("METADATA_BLOCK_PICTURE",EnumSet.of(Tagger.XIPH)),
    SOURCEMEDIA("SOURCEMEDIA",EnumSet.of(Tagger.XIPH)),
    PRODUCTNUMBER("PRODUCTNUMBER",EnumSet.of(Tagger.XIPH)),
    ;

    private String fieldName;
    private EnumSet<Tagger> taggers;

    VorbisCommentFieldKey(String fieldName)
    {
        this.fieldName = fieldName;
    }

    VorbisCommentFieldKey(String fieldName, EnumSet<Tagger> taggers)
    {
        this.fieldName = fieldName;
        this.taggers = taggers;
    }

    public String getFieldName()
    {
        return fieldName;
    }

    /**
     * List of taggers using this field, concentrates primarily on the original tagger to start using a field.
     * Tagger.XIPH means the field is either part  of the Vorbis Standard or a Vorbis proposed extension to the
     * standard
     *
     * @return
     */
    public EnumSet<Tagger> getTaggers()
    {
        return taggers;
    }
}
