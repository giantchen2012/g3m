package org.glob3.mobile.generated; 
//
//  Tile.cpp
//  G3MiOSSDK
//
//  Created by Agustin Trujillo Pino on 12/06/12.
//  Copyright (c) 2012 __MyCompanyName__. All rights reserved.
//

//
//  Tile.hpp
//  G3MiOSSDK
//
//  Created by Agustin Trujillo Pino on 12/06/12.
//  Copyright (c) 2012 __MyCompanyName__. All rights reserved.
//



//class TileTexturizer;
//class Mesh;
//class TileElevationDataRequest;
//class BoundingVolume;
//class Vector3D;
//class TilesRenderParameters;
//class LayerTilesRenderParameters;
//class Frustum;
//class TilesStatistics;
//class ElevationDataProvider;
//class ITimer;
//class GLState;
//class LayerSet;
//class ITexturizerData;
//class PlanetTileTessellatorData;
//class PlanetRenderer;
//class TileKey;
//class Geodetic3D;


public class Tile
{
  private TileTexturizer _texturizer;
  private Tile _parent;

  private Mesh _tessellatorMesh;

  private Mesh _debugMesh;
  private Mesh _texturizedMesh;
  private TileElevationDataRequest _elevationDataRequest;

  private Mesh _flatColorMesh;

  private boolean _textureSolved;
  private java.util.ArrayList<Tile> _subtiles;
  private boolean _justCreatedSubtiles;

  private boolean _texturizerDirty;

  private float _verticalExaggeration;
  private TileTessellatorMeshData _tileTessellatorMeshData = new TileTessellatorMeshData();

  private BoundingVolume _boundingVolume;

  private Vector3D _northWestPoint;
  private Vector3D _northEastPoint;
  private Vector3D _southWestPoint;
  private Vector3D _southEastPoint;

  private static double getSquaredArcSegmentRatio(Vector3D a, Vector3D b)
  {
    /*
     Arco = ang * Cuerda / (2 * sen(ang/2))
     */
  
    final double angleInRadians = Vector3D.angleInRadiansBetween(a, b);
    final double halfAngleSin = java.lang.Math.sin(angleInRadians / 2);
    final double arcSegmentRatio = (halfAngleSin == 0) ? 1 : angleInRadians / (2 * halfAngleSin);
    return (arcSegmentRatio * arcSegmentRatio);
  }

  private void computeTileCorners(Planet planet)
  {
  
    if (_tessellatorMesh == null)
    {
      ILogger.instance().logError("Error in Tile::computeTileCorners");
      return;
    }
  
    if (_northWestPoint != null)
       _northWestPoint.dispose();
    if (_northEastPoint != null)
       _northEastPoint.dispose();
    if (_southWestPoint != null)
       _southWestPoint.dispose();
    if (_southEastPoint != null)
       _southEastPoint.dispose();
  
  
    final double mediumHeight = _tileTessellatorMeshData._averageHeight;
  
    _northWestPoint = new Vector3D(planet.toCartesian(_sector.getNW(), mediumHeight));
    _northEastPoint = new Vector3D(planet.toCartesian(_sector.getNE(), mediumHeight));
    _southWestPoint = new Vector3D(planet.toCartesian(_sector.getSW(), mediumHeight));
    _southEastPoint = new Vector3D(planet.toCartesian(_sector.getSE(), mediumHeight));
  }

  private double _northArcSegmentRatioSquared;
  private double _southArcSegmentRatioSquared;
  private double _eastArcSegmentRatioSquared;
  private double _westArcSegmentRatioSquared;


  private void prepareTestLODData(Planet planet)
  {
  
    if ((_northWestPoint == null) || (_northEastPoint == null) || (_southWestPoint == null) || (_southEastPoint == null))
    {
      ILogger.instance().logError("Error in Tile::prepareTestLODData");
      return;
    }
  
    final Vector3D normalNW = planet.centricSurfaceNormal(_northWestPoint);
    final Vector3D normalNE = planet.centricSurfaceNormal(_northEastPoint);
    final Vector3D normalSW = planet.centricSurfaceNormal(_southWestPoint);
    final Vector3D normalSE = planet.centricSurfaceNormal(_southEastPoint);
  
    _northArcSegmentRatioSquared = getSquaredArcSegmentRatio(normalNW, normalNE);
    _southArcSegmentRatioSquared = getSquaredArcSegmentRatio(normalSW, normalSE);
    _eastArcSegmentRatioSquared = getSquaredArcSegmentRatio(normalNE, normalSE);
    _westArcSegmentRatioSquared = getSquaredArcSegmentRatio(normalNW, normalSW);
  }

  private Mesh getTessellatorMesh(G3MRenderContext rc, ElevationDataProvider elevationDataProvider, TileTessellator tessellator, LayerTilesRenderParameters layerTilesRenderParameters, TilesRenderParameters tilesRenderParameters, long elevationDataRequestPriority)
  {
  
    if (!canUseElevationDataProvider(elevationDataProvider))
    {
      //Marking as ED resolved
      _elevationDataLevel = _level;
    }
    else
    {
  
      if (_elevationData == null)
      {
  
        initializeElevationData(elevationDataProvider, tessellator, layerTilesRenderParameters._tileMeshResolution, rc.getPlanet(), tilesRenderParameters._renderDebug, elevationDataRequestPriority);
  
        if (_elevationData == null)
        {
          //      ILogger::instance()->logInfo("Tile not ready for rendering as no ElevationData can be found. Returning NULL Mesh.");
          return null;
        }
      }
    }
  
    if (_elevationDataLevel < 0 && canUseElevationDataProvider(elevationDataProvider))
    {
      ILogger.instance().logError("Creating Mesh without ED.");
    }
  
    final boolean mustUpdate = (_elevationDataLevelOfTessellatorMesh < _elevationDataLevel);
  
    if ((_tessellatorMesh == null) || mustUpdate)
    {
  
      if (mustUpdate && _tessellatorMesh != null)
      {
        //ILogger::instance()->logInfo("Updating mesh due to new Elevation Data");
  
        //Tessellator mesh is going to change, thus reference to _boundingVolume is sent to oblivion
  
        final Planet planet = rc.getPlanet();
  
        tessellator.updateSurface(_tessellatorMesh, this, layerTilesRenderParameters._tileMeshResolution, planet, _elevationData, _verticalExaggeration, _tileTessellatorMeshData);
  
        computeTileCorners(planet);
        prepareTestLODData(planet);
  
        _boundingVolume = null;
      }
      else
      {
  
        if (elevationDataProvider == null)
        {
          // no elevation data provider, just create a simple mesh without elevation
          _tessellatorMesh = tessellator.createTileMesh(rc.getPlanet(), layerTilesRenderParameters._tileMeshResolution, this, null, _verticalExaggeration, tilesRenderParameters._renderDebug, _tileTessellatorMeshData);
  
          computeTileCorners(rc.getPlanet());
  
        }
        else
        {
          Mesh tessellatorMesh = tessellator.createTileMesh(rc.getPlanet(), layerTilesRenderParameters._tileMeshResolution, this, _elevationData, _verticalExaggeration, tilesRenderParameters._renderDebug, _tileTessellatorMeshData);
  
          MeshHolder meshHolder = (MeshHolder) _tessellatorMesh;
          if (meshHolder == null)
          {
            meshHolder = new MeshHolder(tessellatorMesh);
            _tessellatorMesh = meshHolder;
          }
          else
          {
            meshHolder.setMesh(tessellatorMesh);
          }
  
          computeTileCorners(rc.getPlanet());
        }
      }
  
      _elevationDataLevelOfTessellatorMesh = _elevationDataLevel;
  
      //Notifying when the tile is first created and every time the elevation data changes
      _planetRenderer.sectorElevationChanged(_elevationData);
    }
  
    return _tessellatorMesh;
  }

  private Mesh getDebugMesh(G3MRenderContext rc, TileTessellator tessellator, LayerTilesRenderParameters layerTilesRenderParameters)
  {
    if (_debugMesh == null)
    {
      final Vector2I tileMeshResolution = new Vector2I(layerTilesRenderParameters._tileMeshResolution);
  
      _debugMesh = tessellator.createTileDebugMesh(rc.getPlanet(), tileMeshResolution, this);
    }
    return _debugMesh;
  }

  private boolean isVisible(G3MRenderContext rc, Frustum cameraFrustumInModelCoordinates, ElevationDataProvider elevationDataProvider, Sector renderedSector, TileTessellator tessellator, LayerTilesRenderParameters layerTilesRenderParameters, TilesRenderParameters tilesRenderParameters, long elevationDataRequestPriority)
  {
    if ((renderedSector != null) && !renderedSector.touchesWith(_sector)) //Incomplete world
    {
      return false;
    }
  
    final BoundingVolume boundingVolume = getBoundingVolume(rc, elevationDataProvider, tessellator, layerTilesRenderParameters, tilesRenderParameters, elevationDataRequestPriority);
  
    return ((boundingVolume != null) && boundingVolume.touchesFrustum(cameraFrustumInModelCoordinates));
  }

  private boolean _lastMeetsRenderCriteriaResult;
  private double _lastMeetsRenderCriteriaTimeInMS;

  private boolean meetsRenderCriteria(G3MRenderContext rc, LayerTilesRenderParameters layerTilesRenderParameters, TileTexturizer texturizer, TilesRenderParameters tilesRenderParameters, TilesStatistics tilesStatistics, ITimer lastSplitTimer, double texWidthSquared, double texHeightSquared, double nowInMS)
  {
  
    if ((_level >= layerTilesRenderParameters._maxLevelForPoles) && (_sector.touchesPoles()))
    {
      return true;
    }
  
    if (_level >= layerTilesRenderParameters._maxLevel)
    {
      return true;
    }
  
    if (texturizer != null)
    {
      if (texturizer.tileMeetsRenderCriteria(this))
      {
        return true;
      }
    }
  
    if (_lastMeetsRenderCriteriaTimeInMS != 0 && (nowInMS - _lastMeetsRenderCriteriaTimeInMS) < 250) //500
    {
      return _lastMeetsRenderCriteriaResult;
    }
  
    if (tilesRenderParameters._useTilesSplitBudget)
    {
      if (_subtiles == null) // the tile needs to create the subtiles
      {
        if (lastSplitTimer.elapsedTimeInMilliseconds() < 67)
        {
          // there are not more time-budget to spend
          return true;
        }
      }
    }
  
    _lastMeetsRenderCriteriaTimeInMS = nowInMS; //Storing time of result
//C++ TO JAVA CONVERTER TODO TASK: There is no preprocessor in Java:
//#warning store camera-timestamp to avoid recalculation when the camera isn't moving
  
    final Camera camera = rc.getCurrentCamera();
  
    //LOD Test without mesh
    if (_tessellatorMesh == null)
    {
  
//C++ TO JAVA CONVERTER TODO TASK: There is no preprocessor in Java:
//#warning store camera-timestamp to avoid recalculation when the camera isn't moving
  
      Vector3D ne = rc.getPlanet().toCartesian(_sector.getNE());
      Vector3D sw = rc.getPlanet().toCartesian(_sector.getSW());
      double tileRadius = ne.sub(sw).length() / 2.0;
  
      Vector3D center = rc.getPlanet().toCartesian(_sector.getCenter());
      double distanceToTile = camera.getCartesianPosition().sub(center).length();
      distanceToTile -= tileRadius;
  
      double minTexelSize = getMinimumTexelSideSize(rc.getPlanet(), layerTilesRenderParameters);
      double minTexelSizeInPixels = camera.getPixelsForObjectSize(distanceToTile, minTexelSize);
      if (minTexelSizeInPixels > _planetRenderer.getMaxTexelSizeInPixels())
      {
        System.out.printf("TILE LOD %d DISMISSED\n", _level);
        return false;
      }
    }
  
    //  if ((_northArcSegmentRatioSquared == 0) ||
    //      (_southArcSegmentRatioSquared == 0) ||
    //      (_eastArcSegmentRatioSquared  == 0) ||
    //      (_westArcSegmentRatioSquared  == 0)) {
    //    prepareTestLODData( rc->getPlanet() );
    //  }
    //
    //Computing distance to tile
    double tileRadius = _tileTessellatorMeshData._radius; // _northEastPoint->sub(*_southWestPoint).length() / 2.0;
    //
    //  printf("%f, %f\n", tileRadius, _tileTessellatorMeshData._radius);
    //
    Vector3D center = _tileTessellatorMeshData._meshCenter; //rc->getPlanet()->toCartesian(_sector._center);
    double distanceToTile = camera.getCartesianPosition().distanceTo(center);
    distanceToTile -= tileRadius;
  
    if (distanceToTile < 0) //If we are inside the bounding volume we should split the tile
    {
      return false;
    }
  
    final IMathUtils mu = IMathUtils.instance();
  
    //Deviation
    double visibleDeviation = mu.maxDouble();
    if (distanceToTile > 0.0)
    {
      visibleDeviation = camera.getPixelsForObjectSize(distanceToTile, _tileTessellatorMeshData._deviation);
    }
  
    boolean deviationCriteria = visibleDeviation < _planetRenderer.getMaxDEMDevianceInPixels();
    boolean texelCriteria = false;
  
    if (deviationCriteria)
    {
  
      //Pixel size in meters
      final int texWidth = layerTilesRenderParameters._tileTextureResolution._x; // (int) mu->sqrt(texWidthSquared);
      final int texelsBetweenVerticesLongitude = texWidth / _tileTessellatorMeshData._surfaceResolutionX;
      final double maxTexelWidth = _tileTessellatorMeshData._maxVerticesDistanceInLongitude / texelsBetweenVerticesLongitude;
  
      final int texHeight = layerTilesRenderParameters._tileTextureResolution._y; // (int) mu->sqrt(texHeightSquared);
      final int texelsBetweenVerticesLatitude = texHeight / _tileTessellatorMeshData._surfaceResolutionY;
      final double maxTexelHeight = _tileTessellatorMeshData._maxVerticesDistanceInLatitude / texelsBetweenVerticesLatitude;
  
      final double maxTexelSize = (maxTexelHeight > maxTexelWidth) ? maxTexelHeight : maxTexelWidth;
  
      double maxPixelsPerTexel = mu.maxDouble();
      if (maxPixelsPerTexel > 0.0)
      {
        maxPixelsPerTexel = camera.getPixelsForObjectSize(distanceToTile, maxTexelSize);
      }
  
      texelCriteria = maxPixelsPerTexel < _planetRenderer.getMaxTexelSizeInPixels();
  
    }
  
    //int texHeight = (int) mu->sqrt(texHeightSquared);
  
    //  bool lastLMRCR = _lastMeetsRenderCriteriaResult;
  
    //CRITERIA
    _lastMeetsRenderCriteriaResult = deviationCriteria && texelCriteria;
  
  //  if (_lastMeetsRenderCriteriaResult){
  //    printf("TILE LOD %d RENDERED\n", _level);
  //  }
  
    //  if (_lastMeetsRenderCriteriaResult && !lastLMRCR){
    //    printf("Deviation: %f, Distance: %f, Visible deviation: %f pixels.\nMaxTexelWidth: %f, %f pixels per texel\n",
    //           _tileTessellatorMeshData._deviation,
    //           distanceToTile,
    //           visibleDeviation,
    //           maxTexelWidth,
    //           maxPixelsPerTexel);
    //  }
  
    return _lastMeetsRenderCriteriaResult;
  }

  private void rawRender(G3MRenderContext rc, GLState glState, TileTexturizer texturizer, ElevationDataProvider elevationDataProvider, TileTessellator tessellator, LayerTilesRenderParameters layerTilesRenderParameters, LayerSet layerSet, TilesRenderParameters tilesRenderParameters, boolean forceFullRender, long tileDownloadPriority, boolean logTilesPetitions)
  {
  
    Mesh tessellatorMesh = getTessellatorMesh(rc, elevationDataProvider, tessellator, layerTilesRenderParameters, tilesRenderParameters, tileDownloadPriority);
    if (tessellatorMesh == null)
    {
      return;
    }
  
    if (texturizer == null)
    {
      tessellatorMesh.render(rc, glState);
    }
    else
    {
      final boolean needsToCallTexturizer = (_texturizedMesh == null) || isTexturizerDirty();
  
      if (needsToCallTexturizer)
      {
        _texturizedMesh = texturizer.texturize(rc, tessellator, layerTilesRenderParameters, layerSet, forceFullRender, tileDownloadPriority, this, tessellatorMesh, _texturizedMesh, logTilesPetitions);
      }
  
      if (_texturizedMesh != null)
      {
        _texturizedMesh.render(rc, glState);
      }
      else
      {
        //Adding flat color if no texture set on the mesh
        if (_flatColorMesh == null)
        {
          _flatColorMesh = new FlatColorMesh(tessellatorMesh, false, Color.newFromRGBA((float) 1.0, (float) 1.0, (float) 1.0, (float) 1.0), true);
        }
        _flatColorMesh.render(rc, glState);
      }
    }
  
    //  const BoundingVolume* boundingVolume = getBoundingVolume(rc, trc);
    //  boundingVolume->render(rc, parentState);
  }

  private void debugRender(G3MRenderContext rc, GLState glState, TileTessellator tessellator, LayerTilesRenderParameters layerTilesRenderParameters)
  {
    Mesh debugMesh = getDebugMesh(rc, tessellator, layerTilesRenderParameters);
    if (debugMesh != null)
    {
      debugMesh.render(rc, glState);
    }
  }

  private Tile createSubTile(Angle lowerLat, Angle lowerLon, Angle upperLat, Angle upperLon, int level, int row, int column, boolean setParent)
  {
    Tile parent = setParent ? this : null;
    return new Tile(_texturizer, parent, new Sector(new Geodetic2D(lowerLat, lowerLon), new Geodetic2D(upperLat, upperLon)), _mercator, level, row, column, _planetRenderer);
  }


  private java.util.ArrayList<Tile> getSubTiles(Angle splitLatitude, Angle splitLongitude)
  {
    if (_subtiles == null)
    {
      _subtiles = createSubTiles(splitLatitude, splitLongitude, true);
      _justCreatedSubtiles = true;
    }
    return _subtiles;
  }

//C++ TO JAVA CONVERTER TODO TASK: The implementation of the following method could not be found:
//  Tile(Tile that);

  private void ancestorTexturedSolvedChanged(Tile ancestor, boolean textureSolved)
  {
    if (textureSolved && isTextureSolved())
    {
      return;
    }
  
    if (_texturizer != null)
    {
      _texturizer.ancestorTexturedSolvedChanged(this, ancestor, textureSolved);
    }
  
    if (_subtiles != null)
    {
      final int subtilesSize = _subtiles.size();
      for (int i = 0; i < subtilesSize; i++)
      {
        Tile subtile = _subtiles.get(i);
        subtile.ancestorTexturedSolvedChanged(ancestor, textureSolved);
      }
    }
  }

  private boolean _isVisible;
  private void setIsVisible(boolean isVisible, TileTexturizer texturizer)
  {
    if (_isVisible != isVisible)
    {
      _isVisible = isVisible;
  
      if (!_isVisible)
      {
        deleteTexturizedMesh(texturizer);
      }
    }
  }

  private void deleteTexturizedMesh(TileTexturizer texturizer)
  {
    // check for (_parent != NULL) to avoid deleting the firstLevel tiles.
    // in this case, the mesh is always loaded (as well as its texture) to be the last option
    // falback texture for any tile
    if ((_parent != null) && (_texturizedMesh != null))
    {
  
      if (texturizer != null)
      {
        texturizer.tileMeshToBeDeleted(this, _texturizedMesh);
      }
  
      if (_texturizedMesh != null)
         _texturizedMesh.dispose();
      _texturizedMesh = null;
  
      if (_texturizerData != null)
         _texturizerData.dispose();
      _texturizerData = null;
  
      setTexturizerDirty(true);
      setTextureSolved(false);
    }
  }

  private ITexturizerData _texturizerData;
  private PlanetTileTessellatorData _tessellatorData;

  private int _elevationDataLevel;
  private ElevationData _elevationData;
  private int _elevationDataLevelOfTessellatorMesh;
  private ElevationDataProvider _lastElevationDataProvider;
  private int _lastTileMeshResolutionX;
  private int _lastTileMeshResolutionY;

  private final PlanetRenderer _planetRenderer;

  private BoundingVolume getBoundingVolume(G3MRenderContext rc, ElevationDataProvider elevationDataProvider, TileTessellator tessellator, LayerTilesRenderParameters layerTilesRenderParameters, TilesRenderParameters tilesRenderParameters, long elevationDataRequestPriority)
  {
  
    if (_boundingVolume == null)
    {
      Mesh mesh = getTessellatorMesh(rc, elevationDataProvider, tessellator, layerTilesRenderParameters, tilesRenderParameters, elevationDataRequestPriority);
      if (mesh != null)
      {
        _boundingVolume = mesh.getBoundingVolume();
      }
    }
    //  }
    return null;
  }

  private boolean _rendered;

  private static String createTileId(int level, int row, int column)
  {
    return level + "/" + row + "/" + column;
  }


  private double getMinimumTexelSideSize(Planet planet, LayerTilesRenderParameters layerTilesRenderParameters)
  {
  
    Vector3D ne = planet.toCartesian(_sector.getNE());
    Vector3D nw = planet.toCartesian(_sector.getNW());
    Vector3D se = planet.toCartesian(_sector.getSE());
    //Vector3D sw = planet->toCartesian(_sector.getSW());
  
  
    double lon = ne.sub(nw).length();
    double lat = se.sub(ne).length();
  
    double texelLon = lon / layerTilesRenderParameters.defaultTileTextureResolution()._x;
    double texelLat = lat / layerTilesRenderParameters.defaultTileTextureResolution()._y;
  
    if (texelLat > texelLon)
    {
      return texelLat;
    }
    else
    {
      return texelLon;
    }
  }

  public final Sector _sector ;
  public final boolean _mercator;
  public final int _level;
  public final int _row;
  public final int _column;
  public final String _id;

  public Tile(TileTexturizer texturizer, Tile parent, Sector sector, boolean mercator, int level, int row, int column, PlanetRenderer planetRenderer)
  //_boundingVolume(NULL),
  //_northWestPoint(NULL),
  //_northEastPoint(NULL),
  //_southWestPoint(NULL),
  //_southEastPoint(NULL),
  //_northArcSegmentRatioSquared(0),
  //_southArcSegmentRatioSquared(0),
  //_eastArcSegmentRatioSquared(0),
  //_westArcSegmentRatioSquared(0),
  {
     _texturizer = texturizer;
     _parent = parent;
     _sector = new Sector(sector);
     _mercator = mercator;
     _level = level;
     _row = row;
     _column = column;
     _tessellatorMesh = null;
     _debugMesh = null;
     _flatColorMesh = null;
     _texturizedMesh = null;
     _textureSolved = false;
     _texturizerDirty = true;
     _subtiles = null;
     _justCreatedSubtiles = false;
     _isVisible = false;
     _texturizerData = null;
     _elevationData = null;
     _elevationDataLevel = -1;
     _elevationDataRequest = null;
     _verticalExaggeration = 0F;
     _elevationDataLevelOfTessellatorMesh = -1;
     _lastTileMeshResolutionX = -1;
     _lastTileMeshResolutionY = -1;
     _lastMeetsRenderCriteriaTimeInMS = 0;
     _planetRenderer = planetRenderer;
     _tessellatorData = null;
     _rendered = false;
     _id = createTileId(level, row, column);
  }

  public void dispose()
  {
    if (_debugMesh != null)
       _debugMesh.dispose();
    _debugMesh = null;
  
    if (_flatColorMesh != null)
       _flatColorMesh.dispose();
    _flatColorMesh = null;
  
    if (_tessellatorMesh != null)
       _tessellatorMesh.dispose();
    _tessellatorMesh = null;
  
    if (_texturizerData != null)
       _texturizerData.dispose();
    _texturizerData = null;
  
    if (_texturizedMesh != null)
       _texturizedMesh.dispose();
    _texturizedMesh = null;
  
    if (_elevationData != null)
    {
      _elevationData._release();
      _elevationData = null;
    }
  
    if (_elevationDataRequest != null)
    {
      _elevationDataRequest.cancelRequest(); //The listener will auto delete
      if (_elevationDataRequest != null)
         _elevationDataRequest.dispose();
      _elevationDataRequest = null;
    }
  
    if (_tessellatorData != null)
       _tessellatorData.dispose();
    //
    //  delete _northWestPoint;
    //  delete _northEastPoint;
    //  delete _southWestPoint;
    //  delete _southEastPoint;
  }

  //Change to public for TileCache
  public final java.util.ArrayList<Tile> getSubTiles()
  {
    if (_subtiles != null)
    {
      // quick check to avoid splitLongitude/splitLatitude calculation
      return _subtiles;
    }
  
    final Geodetic2D lower = _sector._lower;
    final Geodetic2D upper = _sector._upper;
  
    final Angle splitLongitude = Angle.midAngle(lower._longitude, upper._longitude);
  
  
    final Angle splitLatitude = _mercator ? MercatorUtils.calculateSplitLatitude(lower._latitude, upper._latitude) : Angle.midAngle(lower._latitude, upper._latitude);
    /*                               */
    /*                               */
  
    return getSubTiles(splitLatitude, splitLongitude);
  }

  public final Mesh getTexturizedMesh()
  {
    return _texturizedMesh;
  }

  public final Tile getParent()
  {
    return _parent;
  }

  public final void prepareForFullRendering(G3MRenderContext rc, TileTexturizer texturizer, ElevationDataProvider elevationDataProvider, TileTessellator tessellator, LayerTilesRenderParameters layerTilesRenderParameters, LayerSet layerSet, TilesRenderParameters tilesRenderParameters, boolean forceFullRender, long tileDownloadPriority, float verticalExaggeration, boolean logTilesPetitions)
  {
  
    //You have to set _verticalExaggertion
    if (verticalExaggeration != _verticalExaggeration)
    {
      // TODO: verticalExaggeration changed, invalidate tileExtent, Mesh, etc.
      _verticalExaggeration = verticalExaggeration;
    }
  
  
    Mesh tessellatorMesh = getTessellatorMesh(rc, elevationDataProvider, tessellator, layerTilesRenderParameters, tilesRenderParameters, tileDownloadPriority);
    if (tessellatorMesh == null)
    {
      return; //Normally due to ElevationData not resolved
    }
  
    if (texturizer != null)
    {
      final boolean needsToCallTexturizer = (_texturizedMesh == null) || isTexturizerDirty();
  
      if (needsToCallTexturizer)
      {
        _texturizedMesh = texturizer.texturize(rc, tessellator, layerTilesRenderParameters, layerSet, forceFullRender, tileDownloadPriority, this, tessellatorMesh, _texturizedMesh, logTilesPetitions);
      }
    }
  }

  public final void updateQuadTree(G3MRenderContext rc, java.util.LinkedList<Tile> renderedTiles, Planet planet, Vector3D cameraNormalizedPosition, double cameraAngle2HorizonInRadians, Frustum cameraFrustumInModelCoordinates, TilesStatistics tilesStatistics, float verticalExaggeration, LayerTilesRenderParameters layerTilesRenderParameters, TileTexturizer texturizer, TilesRenderParameters tilesRenderParameters, ITimer lastSplitTimer, ElevationDataProvider elevationDataProvider, TileTessellator tessellator, LayerSet layerSet, Sector renderedSector, boolean isForcedFullRender, long texturePriority, double texWidthSquared, double texHeightSquared, double nowInMS, long tileDownloadPriority, java.util.ArrayList<Tile> tilesStartedRendering, java.util.ArrayList<String> tilesStoppedRendering)
  {
  
    tilesStatistics.computeTileProcessed(this);
  
    if (verticalExaggeration != _verticalExaggeration)
    {
      // TODO: verticalExaggeration changed, invalidate tileExtent, Mesh, etc.
      _verticalExaggeration = verticalExaggeration;
    }
  
    final boolean tileVisible = isVisible(rc, cameraFrustumInModelCoordinates, elevationDataProvider, renderedSector, tessellator, layerTilesRenderParameters, tilesRenderParameters);
  
    final boolean tileMeetsRenderCriteria = meetsRenderCriteria(rc, layerTilesRenderParameters, texturizer, tilesRenderParameters, tilesStatistics, lastSplitTimer, texWidthSquared, texHeightSquared, nowInMS);
  
    boolean rendered = false;
  
  
    if (isVisible(rc, cameraFrustumInModelCoordinates, elevationDataProvider, renderedSector, tessellator, layerTilesRenderParameters, tilesRenderParameters, tileDownloadPriority))
    {
      setIsVisible(true, texturizer);
  
      tilesStatistics.computeVisibleTile(this);
  
      final boolean isRawRender = (meetsRenderCriteria(rc, layerTilesRenderParameters, texturizer, tilesRenderParameters, tilesStatistics, lastSplitTimer, texWidthSquared, texHeightSquared, nowInMS) || (tilesRenderParameters._incrementalTileQuality && !_textureSolved));
  
      if (isRawRender)
      {
  
        _rendered = true;
  
        renderedTiles.addLast(this);
  
        prune(texturizer, elevationDataProvider, tilesStoppedRendering);
        //TODO: AVISAR CAMBIO DE TERRENO
  
      }
      else
      {
  
        final Geodetic2D lower = _sector._lower;
        final Geodetic2D upper = _sector._upper;
  
        final Angle splitLongitude = Angle.midAngle(lower._longitude, upper._longitude);
  
        final Angle splitLatitude = layerTilesRenderParameters._mercator ? MercatorUtils.calculateSplitLatitude(lower._latitude, upper._latitude) : Angle.midAngle(lower._latitude, upper._latitude);
        /*                               */
        /*                               */
  
        java.util.ArrayList<Tile> subTiles = getSubTiles(splitLatitude, splitLongitude);
        if (_justCreatedSubtiles)
        {
          lastSplitTimer.start();
          tilesStatistics.computeBuilderStartInFrame();
          _justCreatedSubtiles = false;
        }
  
        final int subTilesSize = subTiles.size();
        for (int i = 0; i < subTilesSize; i++)
        {
          Tile subTile = subTiles.get(i);
  
          subTile.updateQuadTree(rc, renderedTiles, planet, cameraNormalizedPosition, cameraAngle2HorizonInRadians, cameraFrustumInModelCoordinates, tilesStatistics, verticalExaggeration, layerTilesRenderParameters, texturizer, tilesRenderParameters, lastSplitTimer, elevationDataProvider, tessellator, layerSet, renderedSector, isForcedFullRender, texturePriority, texWidthSquared, texHeightSquared, nowInMS, tileDownloadPriority, tilesStartedRendering, tilesStoppedRendering);
                                     /* parentState,*/
        }
      }
    }
    else
    {
      setIsVisible(false, texturizer);
  
      prune(texturizer, elevationDataProvider, tilesStoppedRendering);
      //TODO: AVISAR CAMBIO DE TERRENO
    }
  
    if (_rendered != rendered)
    {
      _rendered = rendered;
        if (_rendered)
        {
          if (tilesStartedRendering != null)
          {
            tilesStartedRendering.add(this);
          }
        }
        else
        {
          if (tilesStoppedRendering != null)
          {
            tilesStoppedRendering.add(_id);
          }
        }
    }
  
  }


  public final void performRawRender(G3MRenderContext rc, GLState glState, TileTexturizer texturizer, ElevationDataProvider elevationDataProvider, TileTessellator tessellator, LayerTilesRenderParameters layerTilesRenderParameters, LayerSet layerSet, TilesRenderParameters tilesRenderParameters, boolean isForcedFullRender, long texturePriority, TilesStatistics tilesStatistics, boolean logTilesPetitions)
  {
  
    rawRender(rc, glState, texturizer, elevationDataProvider, tessellator, layerTilesRenderParameters, layerSet, tilesRenderParameters, isForcedFullRender, texturePriority, logTilesPetitions);
    if (tilesRenderParameters._renderDebug) //TO RAW RENDER
    {
      debugRender(rc, glState, tessellator, layerTilesRenderParameters);
    }
  
    tilesStatistics.computeTileRenderered(this);
  
    //TODO: AVISAR CAMBIO DE TERRENO
  }

  public final void zRender(G3MRenderContext rc, GLState parentState)
  {
  
    if (_tessellatorMesh == null)
    {
      ILogger.instance().logError("Calling ZRender for Tile withouth any valid mesh.");
      return;
    }
    else
    {
      _tessellatorMesh.zRender(rc, parentState);
    }
  }

  //const TileKey getKey() const;

  //  const TileKey getKey() const;
  //  const std::string getId() const;


//=======
//
//  void render(const G3MRenderContext* rc,
//              const GLState& parentState,
//              std::vector<Tile*>* toVisitInNextIteration,
//              const Frustum* cameraFrustumInModelCoordinates,
//              TilesStatistics* tilesStatistics,
//              const float verticalExaggeration,
//              const LayerTilesRenderParameters* layerTilesRenderParameters,
//              TileTexturizer* texturizer,
//              const TilesRenderParameters* tilesRenderParameters,
//              ITimer* lastSplitTimer,
//              ElevationDataProvider* elevationDataProvider,
//              const TileTessellator* tessellator,
//              const LayerSet* layerSet,
//              const Sector* renderedSector,
//              bool forceFullRender,
//              long long tileDownloadPriority,
//              double texWidth,
//              double texHeight,
//              double nowInMS,
//              const bool renderTileMeshes,
//              bool logTilesPetitions,
//              std::vector<const Tile*>* tilesStartedRendering,
//              std::vector<std::string>* tilesStoppedRendering);
//
//>>>>>>> purgatory
  public final void setTextureSolved(boolean textureSolved)
  {
    if (textureSolved != _textureSolved)
    {
      _textureSolved = textureSolved;
  
      if (_textureSolved)
      {
        if (_texturizerData != null)
           _texturizerData.dispose();
        _texturizerData = null;
      }
  
      if (_subtiles != null)
      {
        final int subtilesSize = _subtiles.size();
        for (int i = 0; i < subtilesSize; i++)
        {
          Tile subtile = _subtiles.get(i);
          subtile.ancestorTexturedSolvedChanged(this, _textureSolved);
        }
      }
    }
  }

  public final boolean isTextureSolved()
  {
    return _textureSolved;
  }

  public final void setTexturizerDirty(boolean texturizerDirty)
  {
    _texturizerDirty = texturizerDirty;
  }

  public final boolean isTexturizerDirty()
  {
    return _texturizerDirty;
  }

  public final boolean hasTexturizerData()
  {
    return (_texturizerData != null);
  }

  public final ITexturizerData getTexturizerData()
  {
    return _texturizerData;
  }

  public final void setTexturizerData(ITexturizerData texturizerData)
  {
    if (texturizerData != _texturizerData)
    {
      if (_texturizerData != null)
         _texturizerData.dispose();
      _texturizerData = texturizerData;
    }
  }

  public final PlanetTileTessellatorData getTessellatorData()
  {
    return _tessellatorData;
  }

  public final void setTessellatorData(PlanetTileTessellatorData tessellatorData)
  {
    if (tessellatorData != _tessellatorData)
    {
      if (_tessellatorData != null)
         _tessellatorData.dispose();
      _tessellatorData = tessellatorData;
    }
  }

  public final Tile getDeepestTileContaining(Geodetic3D position)
  {
    if (_sector.contains(position._latitude, position._longitude))
    {
      if (_subtiles == null)
      {
        return this;
      }
  
      for (int i = 0; i < _subtiles.size(); i++)
      {
        final Tile subtile = _subtiles.get(i);
        final Tile subtileResult = subtile.getDeepestTileContaining(position);
        if (subtileResult != null)
        {
          return subtileResult;
        }
      }
    }
  
    return null;
  }

  public final void prune(TileTexturizer texturizer, ElevationDataProvider elevationDataProvider, java.util.ArrayList<String> tilesStoppedRendering)
  {
  
    if (_subtiles != null)
    {
      //Notifying elevation event when LOD decreases
      _planetRenderer.sectorElevationChanged(_elevationData);
  
      final int subtilesSize = _subtiles.size();
      for (int i = 0; i < subtilesSize; i++)
      {
        Tile subtile = _subtiles.get(i);
  
        subtile.setIsVisible(false, texturizer);
  
        subtile.prune(texturizer, elevationDataProvider, tilesStoppedRendering);
        if (texturizer != null)
        {
          texturizer.tileToBeDeleted(subtile, subtile._texturizedMesh);
        }
        if (subtile != null)
           subtile.dispose();
      }
  
      _subtiles = null;
      _subtiles = null;
    }
  }

  public final void toBeDeleted(TileTexturizer texturizer, ElevationDataProvider elevationDataProvider, java.util.ArrayList<String> tilesStoppedRendering)
  {
    if (_rendered)
    {
      if (tilesStoppedRendering != null)
      {
        tilesStoppedRendering.add(_id);
      }
    }
  
    prune(texturizer, elevationDataProvider, tilesStoppedRendering);
  
    if (texturizer != null)
    {
      texturizer.tileToBeDeleted(this, _texturizedMesh);
    }
  
    if (elevationDataProvider != null)
    {
      if (_elevationDataRequest != null)
      {
        _elevationDataRequest.cancelRequest();
      }
    }
  }

  public final String description()
  {
    IStringBuilder isb = IStringBuilder.newStringBuilder();
    isb.addString("(Tile");
    isb.addString(" level=");
    isb.addInt(_level);
    isb.addString(", row=");
    isb.addInt(_row);
    isb.addString(", column=");
    isb.addInt(_column);
    isb.addString(", sector=");
    isb.addString(_sector.description());
    isb.addString(")");
    final String s = isb.getString();
    if (isb != null)
       isb.dispose();
    return s;
  }
  @Override
  public String toString() {
    return description();
  }

  public final java.util.ArrayList<Tile> createSubTiles(Angle splitLatitude, Angle splitLongitude, boolean setParent)
  {
    final Geodetic2D lower = _sector._lower;
    final Geodetic2D upper = _sector._upper;
  
    final int nextLevel = _level + 1;
  
    final int row2 = 2 * _row;
    final int column2 = 2 * _column;
  
    java.util.ArrayList<Tile> subTiles = new java.util.ArrayList<Tile>();
  
    final Sector renderedSector = _planetRenderer.getRenderedSector();
  
    Sector s1 = new Sector(new Geodetic2D(lower._latitude, lower._longitude), new Geodetic2D(splitLatitude, splitLongitude));
    if (renderedSector == null || renderedSector.touchesWith(s1))
    {
      subTiles.add(createSubTile(lower._latitude, lower._longitude, splitLatitude, splitLongitude, nextLevel, row2, column2, setParent));
    }
  
    Sector s2 = new Sector(new Geodetic2D(lower._latitude, splitLongitude), new Geodetic2D(splitLatitude, upper._longitude));
    if (renderedSector == null || renderedSector.touchesWith(s2))
    {
      subTiles.add(createSubTile(lower._latitude, splitLongitude, splitLatitude, upper._longitude, nextLevel, row2, column2 + 1, setParent));
    }
  
    Sector s3 = new Sector(new Geodetic2D(splitLatitude, lower._longitude), new Geodetic2D(upper._latitude, splitLongitude));
    if (renderedSector == null || renderedSector.touchesWith(s3))
    {
      subTiles.add(createSubTile(splitLatitude, lower._longitude, upper._latitude, splitLongitude, nextLevel, row2 + 1, column2, setParent));
    }
  
    Sector s4 = new Sector(new Geodetic2D(splitLatitude, splitLongitude), new Geodetic2D(upper._latitude, upper._longitude));
    if (renderedSector == null || renderedSector.touchesWith(s4))
    {
      subTiles.add(createSubTile(splitLatitude, splitLongitude, upper._latitude, upper._longitude, nextLevel, row2 + 1, column2 + 1, setParent));
    }
  
    subTiles.trimToSize();
  
    return subTiles;
  }

  public final boolean isElevationDataSolved()
  {
    return (_elevationDataLevel == _level);
  }

  public final ElevationData getElevationData()
  {
    return _elevationData;
  }

  public final void setElevationData(ElevationData ed, int level)
  {
    if (_elevationDataLevel < level)
    {
  
      if (_elevationData != null)
      {
        _elevationData._release();
        _elevationData = null;
      }
  
      _elevationData = ed;
      _elevationData._retain();
  
      _elevationDataLevel = level;
  
      //If the elevation belongs to tile's level, we notify the sub-tree
      if (isElevationDataSolved())
      {
        if (_subtiles != null)
        {
          final int subtilesSize = _subtiles.size();
          for (int i = 0; i < subtilesSize; i++)
          {
            Tile subtile = _subtiles.get(i);
            subtile.ancestorChangedElevationData(this);
          }
        }
      }
  
    }
  }

  public final void getElevationDataFromAncestor(Vector2I extent)
  {
    if (_elevationData == null)
    {
      Tile ancestor = getParent();
      while ((ancestor != null) && !ancestor.isElevationDataSolved())
      {
        ancestor = ancestor.getParent();
      }
  
      if (ancestor != null)
      {
        ElevationData subView = createElevationDataSubviewFromAncestor(ancestor);
        setElevationData(subView, ancestor._level);
      }
      else
      {
        if (getParent() != null)
        {
          ILogger.instance().logInfo("No ancestor has elevation data.");
        }
      }
    }
  }

  public final void initializeElevationData(ElevationDataProvider elevationDataProvider, TileTessellator tessellator, Vector2I tileMeshResolution, Planet planet, boolean renderDebug, long requestPriority)
  {
  
    if (!canUseElevationDataProvider(elevationDataProvider))
    {
      //Marking data elevation as solved
      _elevationDataLevel = _level;
      return;
    }
  
    //Storing for subviewing
    _lastElevationDataProvider = elevationDataProvider;
    _lastTileMeshResolutionX = tileMeshResolution._x;
    _lastTileMeshResolutionY = tileMeshResolution._y;
    if (_elevationDataRequest == null)
    {
  
      final Vector2I res = tessellator.getTileMeshResolution(planet, tileMeshResolution, this, renderDebug);
      _elevationDataRequest = new TileElevationDataRequest(this, res, requestPriority, elevationDataProvider);
      _elevationDataRequest.sendRequest();
    }
  
    //If after petition we still have no data we request from ancestor (provider asynchronous)
    if (_elevationData == null)
    {
      getElevationDataFromAncestor(tileMeshResolution);
    }
  
  }

  public final void ancestorChangedElevationData(Tile ancestor)
  {
  
    if (ancestor._level > _elevationDataLevel)
    {
      ElevationData subView = createElevationDataSubviewFromAncestor(ancestor);
      if (subView != null)
      {
        setElevationData(subView, ancestor._level);
      }
    }
  
    if (_subtiles != null)
    {
      final int subtilesSize = _subtiles.size();
      for (int i = 0; i < subtilesSize; i++)
      {
        Tile subtile = _subtiles.get(i);
        subtile.ancestorChangedElevationData(this);
      }
    }
  }

  public final ElevationData createElevationDataSubviewFromAncestor(Tile ancestor)
  {
    ElevationData ed = ancestor.getElevationData();
  
    if (ed == null)
    {
      ILogger.instance().logError("Ancestor can't have undefined Elevation Data.");
      return null;
    }
  
    if (ed.getExtentWidth() < 1 || ed.getExtentHeight() < 1)
    {
      ILogger.instance().logWarning("Tile too small for ancestor elevation data.");
      return null;
    }
  
    if ((_lastElevationDataProvider != null) && (_lastTileMeshResolutionX > 0) && (_lastTileMeshResolutionY > 0))
    {
      return ed;
      //    return new DecimatedSubviewElevationData(ed,
      //                                             _sector,
      //                                             Vector2I(_lastTileMeshResolutionX, _lastTileMeshResolutionY));
    }
  
    ILogger.instance().logError("Can't create subview of elevation data from ancestor");
    return null;
  
  }

  //
  //void Tile::prepareTestLODData(const Planet* planet) {
  //
  //  //  if (_middleNorthPoint == NULL) {
  //  //    ILogger::instance()->logError("Error in Tile::prepareTestLODData");
  //  //    return;
  //  //  }
  //  //
  //  //  const Vector3D nN = planet->centricSurfaceNormal(*_middleNorthPoint);
  //  //  const Vector3D nS = planet->centricSurfaceNormal(*_middleSouthPoint);
  //  //  const Vector3D nE = planet->centricSurfaceNormal(*_middleEastPoint);
  //  //  const Vector3D nW = planet->centricSurfaceNormal(*_middleWestPoint);
  //  //
  //  //  const Angle latitudeAngle = nN.angleBetween(nS);
  //  //  double latRad = latitudeAngle._radians;
  //  //  const double sin_lat_2 = SIN(latRad / 2);
  //  //  const double latitudeArcSegmentRatio = (sin_lat_2 == 0) ? 1 : latRad / (2 * sin_lat_2);
  //  //
  //  //  const Angle longitudeAngle = nE.angleBetween(nW);
  //  //  const double lonRad = longitudeAngle._radians;
  //  //  const double sin_lon_2 = SIN(lonRad / 2);
  //  //  const double longitudeArcSegmentRatio = (sin_lon_2 == 0) ? 1 : lonRad / (2 * sin_lon_2);
  //  //
  //  //  _latitudeArcSegmentRatioSquared  = latitudeArcSegmentRatio * latitudeArcSegmentRatio;
  //  //  _longitudeArcSegmentRatioSquared = longitudeArcSegmentRatio * longitudeArcSegmentRatio;
  //
  //  if ((_northWestPoint == NULL) ||
  //      (_northEastPoint == NULL) ||
  //      (_southWestPoint == NULL) ||
  //      (_southEastPoint == NULL)) {
  //    ILogger::instance()->logError("Error in Tile::prepareTestLODData");
  //    return;
  //  }
  //
  //  const Vector3D normalNW = planet->centricSurfaceNormal(*_northWestPoint);
  //  const Vector3D normalNE = planet->centricSurfaceNormal(*_northEastPoint);
  //  const Vector3D normalSW = planet->centricSurfaceNormal(*_southWestPoint);
  //  const Vector3D normalSE = planet->centricSurfaceNormal(*_southEastPoint);
  //
  //  _northArcSegmentRatioSquared = getSquaredArcSegmentRatio(normalNW, normalNE);
  //  _southArcSegmentRatioSquared = getSquaredArcSegmentRatio(normalSW, normalSE);
  //  _eastArcSegmentRatioSquared  = getSquaredArcSegmentRatio(normalNE, normalSE);
  //  _westArcSegmentRatioSquared  = getSquaredArcSegmentRatio(normalNW, normalSW);
  //}
  //
  //double Tile::getSquaredArcSegmentRatio(const Vector3D& a,
  //                                       const Vector3D& b) {
  //  /*
  //   Arco = ang * Cuerda / (2 * sen(ang/2))
  //   */
  //
  //  const double angleInRadians = a.angleInRadiansBetween(b);
  //  const double halfAngleSin = SIN(angleInRadians / 2);
  //  const double arcSegmentRatio = (halfAngleSin == 0) ? 1 : angleInRadians / (2 * halfAngleSin);
  //  return (arcSegmentRatio * arcSegmentRatio);
  //}
  //
  //void Tile::computeTileCorners(const Planet* planet) {
  //
  //  if (_tessellatorMesh == NULL) {
  //    ILogger::instance()->logError("Error in Tile::computeTileCorners");
  //    return;
  //  }
  //
  //  //  delete _middleWestPoint;
  //  //  delete _middleEastPoint;
  //  //  delete _middleNorthPoint;
  //  //  delete _middleSouthPoint;
  //  delete _northWestPoint;
  //  delete _northEastPoint;
  //  delete _southWestPoint;
  //  delete _southEastPoint;
  //
  //
  //  const double mediumHeight = _tileTessellatorMeshData._averageHeight;
  //
  //  //  const Geodetic2D center = _sector.getCenter();
  //  //  const Geodetic3D gN( Geodetic2D(_sector.getNorth(), center._longitude), mediumHeight);
  //  //  const Geodetic3D gS( Geodetic2D(_sector.getSouth(), center._longitude), mediumHeight);
  //  //  const Geodetic3D gW( Geodetic2D(center._latitude, _sector.getWest()), mediumHeight);
  //  //  const Geodetic3D gE( Geodetic2D(center._latitude, _sector.getEast()), mediumHeight);
  //  //
  //  //  _middleNorthPoint = new Vector3D(planet->toCartesian(gN));
  //  //  _middleSouthPoint = new Vector3D(planet->toCartesian(gS));
  //  //  _middleEastPoint = new Vector3D(planet->toCartesian(gE));
  //  //  _middleWestPoint = new Vector3D(planet->toCartesian(gW));
  //
  //  _northWestPoint = new Vector3D( planet->toCartesian( _sector.getNW(), mediumHeight ) );
  //  _northEastPoint = new Vector3D( planet->toCartesian( _sector.getNE(), mediumHeight ) );
  //  _southWestPoint = new Vector3D( planet->toCartesian( _sector.getSW(), mediumHeight ) );
  //  _southEastPoint = new Vector3D( planet->toCartesian( _sector.getSE(), mediumHeight ) );
  //}
  /*
  void Tile::prepareTestLODData(const Planet* planet) {
    if ((_northWestPoint == NULL) ||
        (_northEastPoint == NULL) ||
        (_southWestPoint == NULL) ||
        (_southEastPoint == NULL)) {
      ILogger::instance()->logError("Error in Tile::prepareTestLODData");
      return;
    }
    
    const Vector3D normalNW = planet->centricSurfaceNormal(*_northWestPoint);
    const Vector3D normalNE = planet->centricSurfaceNormal(*_northEastPoint);
    const Vector3D normalSW = planet->centricSurfaceNormal(*_southWestPoint);
    const Vector3D normalSE = planet->centricSurfaceNormal(*_southEastPoint);
    
    _northArcSegmentRatioSquared = getSquaredArcSegmentRatio(normalNW, normalNE);
    _southArcSegmentRatioSquared = getSquaredArcSegmentRatio(normalSW, normalSE);
    _eastArcSegmentRatioSquared  = getSquaredArcSegmentRatio(normalNE, normalSE);
    _westArcSegmentRatioSquared  = getSquaredArcSegmentRatio(normalNW, normalSW);
  }
  
  double Tile::getSquaredArcSegmentRatio(const Vector3D& a,
                                         const Vector3D& b) {
   
     //Arco = ang * Cuerda / (2 * sen(ang/2))
    
    const double angleInRadians = Vector3D::angleInRadiansBetween(a, b);
    const double halfAngleSin = SIN(angleInRadians / 2);
    const double arcSegmentRatio = (halfAngleSin == 0) ? 1 : angleInRadians / (2 * halfAngleSin);
    return (arcSegmentRatio * arcSegmentRatio);
  }
  
  void Tile::computeTileCorners(const Planet* planet) {
    
    if (_tessellatorMesh == NULL) {
      ILogger::instance()->logError("Error in Tile::computeTileCorners");
      return;
    }
    
    delete _northWestPoint;
    delete _northEastPoint;
    delete _southWestPoint;
    delete _southEastPoint;
    
    
    const double mediumHeight = _tileTessellatorMeshData._averageHeight;
    
    _northWestPoint = new Vector3D( planet->toCartesian( _sector.getNW(), mediumHeight ) );
    _northEastPoint = new Vector3D( planet->toCartesian( _sector.getNE(), mediumHeight ) );
    _southWestPoint = new Vector3D( planet->toCartesian( _sector.getSW(), mediumHeight ) );
    _southEastPoint = new Vector3D( planet->toCartesian( _sector.getSE(), mediumHeight ) );
  }
  */
  public final Vector2I getNormalizedPixelsFromPosition(Geodetic2D position2D, Vector2I tileDimension)
  {
    final IMathUtils math = IMathUtils.instance();
    final Vector2D uv = _sector.getUVCoordinates(position2D);
    return new Vector2I(math.toInt(tileDimension._x * uv._x), math.toInt(tileDimension._y * uv._y));
  }

  public final boolean canUseElevationDataProvider(ElevationDataProvider edp)
  {
    return (edp != null && edp.isEnabled() && edp.containsSector(_sector));
  }

}
//C++ TO JAVA CONVERTER TODO TASK: There is no preprocessor in Java:
//#pragma mark ElevationData methods