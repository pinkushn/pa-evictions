package com.lancasterstandsup.evictiondata;

import java.util.List;

import com.itextpdf.text.pdf.parser.LineSegment;
import com.itextpdf.text.pdf.parser.LocationTextExtractionStrategy;
import com.itextpdf.text.pdf.parser.TextExtractionStrategy;
import com.itextpdf.text.pdf.parser.TextRenderInfo;
import com.itextpdf.text.pdf.parser.Vector;

/**
 * <a href="http://stackoverflow.com/questions/33492792/how-can-i-extract-subscript-superscript-properly-from-a-pdf-using-itextsharp">
 * How can I extract subscript / superscript properly from a PDF using iTextSharp?
 * </a>
 * <br/>
 * <a href="http://www.mass.gov/courts/docs/lawlib/300-399cmr/310cmr7.pdf">310cmr7.pdf</a>
 * <p>
 * This {@link TextExtractionStrategy} uses a TextLineFinder to identify
 * horizontal text lines and then uses these informations to sort the text chunks.
 * </p>
 * <p>
 * In Commit 53526e4854fcb80c86cbc2e113f7a07401dc9a67 ("Refactor LocationTextExtractionStrategy...")
 * during 5.5.9-SNAPSHOT the internal representation of the location of a text chunk has been abstracted
 * to allow for changes here without reflection being required. This version is adapted to this
 * abstraction; for older iText versions, cf. HorizontalTextExtractionStrategy.
 * </p>
 *
 * @author mkl
 */
public class HorizontalTextExtractionStrategy2 extends LocationTextExtractionStrategy
{
    public static class HorizontalTextChunkLocationStrategy implements TextChunkLocationStrategy
    {
        public HorizontalTextChunkLocationStrategy(TextLineFinder textLineFinder)
        {
            this.textLineFinder = textLineFinder;
        }

        @Override
        public TextChunkLocation createLocation(TextRenderInfo renderInfo, LineSegment baseline)
        {
            return new HorizontalTextChunkLocation(baseline.getStartPoint(), baseline.getEndPoint(), renderInfo.getSingleSpaceWidth());
        }

        final TextLineFinder textLineFinder;

        public class HorizontalTextChunkLocation implements TextChunkLocation
        {
            /** the starting location of the chunk */
            private final Vector startLocation;
            /** the ending location of the chunk */
            private final Vector endLocation;
            /** unit vector in the orientation of the chunk */
            private final Vector orientationVector;
            /** the orientation as a scalar for quick sorting */
            private final int orientationMagnitude;
            /** perpendicular distance to the orientation unit vector (i.e. the Y position in an unrotated coordinate system)
             * we round to the nearest integer to handle the fuzziness of comparing floats */
            private final int distPerpendicular;
            /** distance of the start of the chunk parallel to the orientation unit vector (i.e. the X position in an unrotated coordinate system) */
            private final float distParallelStart;
            /** distance of the end of the chunk parallel to the orientation unit vector (i.e. the X position in an unrotated coordinate system) */
            private final float distParallelEnd;
            /** the width of a single space character in the font of the chunk */
            private final float charSpaceWidth;

            public HorizontalTextChunkLocation(Vector startLocation, Vector endLocation, float charSpaceWidth)
            {
                this.startLocation = startLocation;
                this.endLocation = endLocation;
                this.charSpaceWidth = charSpaceWidth;

                Vector oVector = endLocation.subtract(startLocation);
                if (oVector.length() == 0)
                {
                    oVector = new Vector(1, 0, 0);
                }
                orientationVector = oVector.normalize();
                orientationMagnitude = (int)(Math.atan2(orientationVector.get(Vector.I2), orientationVector.get(Vector.I1))*1000);

                // see http://mathworld.wolfram.com/Point-LineDistance2-Dimensional.html
                // the two vectors we are crossing are in the same plane, so the result will be purely
                // in the z-axis (out of plane) direction, so we just take the I3 component of the result
                Vector origin = new Vector(0,0,1);
                distPerpendicular = (int)(startLocation.subtract(origin)).cross(orientationVector).get(Vector.I3);

                distParallelStart = orientationVector.dot(startLocation);
                distParallelEnd = orientationVector.dot(endLocation);
            }

            public int orientationMagnitude()   {   return orientationMagnitude;    }
            public int distPerpendicular()      {   return distPerpendicular;       }
            public float distParallelStart()    {   return distParallelStart;       }
            public float distParallelEnd()      {   return distParallelEnd;         }
            public Vector getStartLocation()    {   return startLocation;           }
            public Vector getEndLocation()      {   return endLocation;             }
            public float getCharSpaceWidth()    {   return charSpaceWidth;          }

            /**
             * @param as the location to compare to
             * @return true is this location is on the the same line as the other
             */
            public boolean sameLine(TextChunkLocation as)
            {
                if (as instanceof HorizontalTextChunkLocation)
                {
                    HorizontalTextChunkLocation horAs = (HorizontalTextChunkLocation) as;
                    return getLineNumber() == horAs.getLineNumber();
                }
                else
                    return orientationMagnitude() == as.orientationMagnitude() && distPerpendicular() == as.distPerpendicular();
            }

            /**
             * Computes the distance between the end of 'other' and the beginning of this chunk
             * in the direction of this chunk's orientation vector.  Note that it's a bad idea
             * to call this for chunks that aren't on the same line and orientation, but we don't
             * explicitly check for that condition for performance reasons.
             * @param other
             * @return the number of spaces between the end of 'other' and the beginning of this chunk
             */
            public float distanceFromEndOf(TextChunkLocation other)
            {
                float distance = distParallelStart() - other.distParallelEnd();
                return distance;
            }

            public boolean isAtWordBoundary(TextChunkLocation previous)
            {
                /**
                 * Here we handle a very specific case which in PDF may look like:
                 * -.232 Tc [( P)-226.2(r)-231.8(e)-230.8(f)-238(a)-238.9(c)-228.9(e)]TJ
                 * The font's charSpace width is 0.232 and it's compensated with charSpacing of 0.232.
                 * And a resultant TextChunk.charSpaceWidth comes to TextChunk constructor as 0.
                 * In this case every chunk is considered as a word boundary and space is added.
                 * We should consider charSpaceWidth equal (or close) to zero as a no-space.
                 */
                if (getCharSpaceWidth() < 0.1f)
                    return false;

                float dist = distanceFromEndOf(previous);

                return dist < -getCharSpaceWidth() || dist > getCharSpaceWidth()/2.0f;
            }

            public int getLineNumber()
            {
                Vector startLocation = getStartLocation();
                float y = startLocation.get(Vector.I2);
                List<Float> flips = textLineFinder.verticalFlips;
                if (flips == null || flips.isEmpty())
                    return 0;
                if (y < flips.get(0))
                    return flips.size() / 2 + 1;
                for (int i = 1; i < flips.size(); i+=2)
                {
                    if (y < flips.get(i))
                    {
                        return (1 + flips.size() - i) / 2;
                    }
                }
                return 0;
            }

            @Override
            public int compareTo(TextChunkLocation rhs)
            {
                if (rhs instanceof HorizontalTextChunkLocation)
                {
                    HorizontalTextChunkLocation horRhs = (HorizontalTextChunkLocation) rhs;
                    int rslt = Integer.compare(getLineNumber(), horRhs.getLineNumber());
                    if (rslt != 0) return rslt;
                    return Float.compare(getStartLocation().get(Vector.I1), rhs.getStartLocation().get(Vector.I1));
                }
                else
                {
                    int rslt;
                    rslt = Integer.compare(orientationMagnitude(), rhs.orientationMagnitude());
                    if (rslt != 0) return rslt;

                    rslt = Integer.compare(distPerpendicular(), rhs.distPerpendicular());
                    if (rslt != 0) return rslt;

                    return Float.compare(distParallelStart(), rhs.distParallelStart());
                }
            }
        }
    }

    @Override
    public void renderText(TextRenderInfo renderInfo)
    {
        textLineFinder.renderText(renderInfo);
        super.renderText(renderInfo);
    }

    public HorizontalTextExtractionStrategy2() throws NoSuchFieldException, SecurityException
    {
        this(new TextLineFinder());
    }

    public HorizontalTextExtractionStrategy2(TextLineFinder textLineFinder) throws NoSuchFieldException, SecurityException
    {
        super(new HorizontalTextChunkLocationStrategy(textLineFinder));

        this.textLineFinder = textLineFinder;
    }

    final TextLineFinder textLineFinder;
}