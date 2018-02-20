# global_raster_tools

## Introduction

This is some Java code for quite a specific set of problems, involving shapefiles, raster images, and raster population data.
It requires any Java installation, and quite a lot of RAM to do most of its useful tasks.

## Documentation

Clone this repo and view the docs folder for JavaDoc documentation of the main class. But briefly, here are some of the useful things
it can do.


```
GlobalRasterTools GRT = new GlobalRaterTools();
GRT.downloadShapeFiles("C:\\MyShapefiles");
```
Initialises, and then downloads an entire set of global shapefiles from GADM into the specified path.

```
GRT.loadPolygonFolder("C:\\MyShapefiles", 2, null);
```
Load all the polygons down in the to admin level 2 where available. To limit the countries loaded, change ```null``` to a 
List<String>, containing country strings that GADM likes. Guessing these strings is not trivial, however...

```
GRT.saveUnits("C:\\units.txt")
GRT.loadUnits("C:\\units.txt")
```
... allow saving and reloading all the unit meta-data, so see those files for a list of the names and GADM ids for the admin hierarchy.

```
GRT.makeMap()
GRT.saveMapFile("C:\\map.bin")
GRT.loadMapFile("C:\\map.bin")
```
Rasterises all the polygons you've got loaded onto a 1/120 degree resolution grid - 43200 x 21600. If multiple polygons claim the
same pixel (whch they pretty much always will on their boundaries, since polygon resolution is much finer than our pixels), the
polygon with the biggest geographical share of the pixel gets it - which is fair enoguh since we can only assume equal distribution
of population within a landscan or GRUMP grid-square. You can then save the map and reload it - which takes a bit of time, but may
be quicker than re-rasterising...

```
double[] result = GRT.getCentroid(id, pop_weighted, pop)
```
Calculate the centroid of a shape. ```id``` is the index into units.txt for the unit you want; pop_weighted is true if you want
a population-weighted centroid, or false to assume equally distributed population, and ```pop``` is a grid of the same dimensions
and orientation as your map file, giving population in those squares. The result is three doubles: the longitude, latitude, and
population (or number of cells) in that unit.

```
int id = GRT.getNearest(lon, lat)
```
Many examples occur when a population density dataset claims there are people living in a cell which is not within any polygon.
Usually, these are aliasing issues on a water border. Use this function to find the nearest admin unit to a given co-ordinate.

## Compiling

Nothing special.

```
javac com/mrc/GlobalRasterTools/*.java
javac examples/GaviCountries/*.java
javac examples/LorenzoTiles/*.java

```

## Running the examples

Similarly, nothing very difficult here - although I'm not sure whether you really want to run these. Note the high memory
usage - the nature of my polygon filling approach, and more generally when working at 43200x21600 resolution.

```
java -Xmx20g examples.GaviCountries.GaviCountries
java -Xmx20g examples.LorenzoTiles.LorenzoTiles
```

