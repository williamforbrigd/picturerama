package Scenes;

import Components.FileLogger;
import Components.PhotoViewer;
import Components.UserInfo;
import Css.Css;
import Database.HibernateClasses.Photo;
import java.util.List;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Level;

/**
 * Class for the map scene
 */
final class MapScene extends SceneBuilder {

  private final List<Photo> PHOTO_LIST = new ArrayList<>();
  private final WebView WEB_VIEW = new WebView();
  private final StackPane STACK_PANE = new StackPane();
  private final MapBridge MAP_BRIDGE = new MapBridge();

  /**
   * Instantiates a new Map scene.
   */
  MapScene() {
    super();
    PHOTO_LIST.addAll(UserInfo.getUser().getPhotos());
    this.setLayout();
  }

  /**
   * Sets the layout of the Map scene. The setLayout()-method from SceneBuilder is overridden, but also
   * called in the method in order to modify the method.
   */
  @Override
  void setLayout() {
    super.setLayout();
    super.setPageTitle("Map");
    super.getGridPane().add(STACK_PANE, 0, 0);
    this.setUpMap();
  }

  /**
   * Sets up the the webview by loading the html and initialising a listener which makes it possible to call methods in MapBridge from the javascript
   */
  private void setUpMap() {
    WEB_VIEW.getEngine().loadContent(getHtml(), "text/html");
    WEB_VIEW.getEngine().getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
      JSObject window = (JSObject) WEB_VIEW.getEngine().executeScript("window");
      window.setMember("java", MAP_BRIDGE);
    });
    Rectangle r = new Rectangle();
    r.widthProperty().bind(WEB_VIEW.widthProperty());
    r.heightProperty().bind(WEB_VIEW.heightProperty());
    r.setArcWidth(30);
    r.setArcHeight(30);
    WEB_VIEW.setClip(r);
    STACK_PANE.setPrefWidth(700.0);
    Css.setMapPane(STACK_PANE);
    STACK_PANE.getChildren().add(WEB_VIEW);
  }

  /**
   * The type Map bridge.
   */
  public final class MapBridge {
    /**
     * Open PhotoViewer with the given photo id
     *
     * @param photo_id the photo id of the photo
     */
    public final void open(int photo_id) {
      Photo photo = findPhotoById(photo_id);
      if (photo != null) {
        PhotoViewer photoViewer = new PhotoViewer(photo);
        photoViewer.display();
      }
    }

    /**
     * Find the Photo with a specific photo id
     *
     * @param photo_id id of photo
     * @return photo if found, else null
     */
    private Photo findPhotoById(int photo_id) {
      for (Photo photo : PHOTO_LIST) {
        if (photo.getId() == photo_id) {
          return photo;
        }
      }
      return null;
    }
  }

  /**
   * Get a html string which creates a map with markers where the uploaded photos was photographed
   *
   * @return string with html
   */
  private String getHtml() {
    StringBuilder html = new StringBuilder();
    html.append("<!DOCTYPE html>");
    html.append("<html>");
    html.append("<head>");
    html.append("<meta name=\"viewport\" content=\"initial-scale=1.0, user-scalable=no\" />");
    html.append("<meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\"/>");
    html.append("<style>html,body{height:100%;margin:0;padding:0;}#map_canvas{height:100%}</style>");
    html.append("<script type=\"text/javascript\" src=\"https://maps.googleapis.com/maps/api/js?sensor=false").append(getApiKey()).append("\"></script>");
    html.append("<script type=\"text/javascript\">");
    html.append("function initialise() {");
    html.append("  var options = { zoom:2, mapTypeId:google.maps.MapTypeId.ROADMAP, center:new google.maps.LatLng(0.0, 0.0), mapTypeControl: true, mapTypeControlOptions: {mapTypeIds: ['roadmap', 'hybrid']}, scaleControl: false, streetViewControl: false, rotateControl: false, fullscreenControl: false};");
    html.append("  var map = new google.maps.Map(document.getElementById('map_canvas'), options);");
    html.append("  var marker;");
    html.append("  var bounds = new google.maps.LatLngBounds();");

    for (Photo photo : PHOTO_LIST) {
      // Only add photos which has a location stored to the map
      if (photo.getLatitude() != null && photo.getLongitude() != null) {
        String location = "new google.maps.LatLng(" + photo.getLatitude() + "," + photo.getLongitude() + ")";
        html.append("  var image = {url: '").append(photo.getUrl()).append("', scaledSize: new google.maps.Size(").append(getMarkerSize(photo)).append("), origin: new google.maps.Point(0, 0), anchor: new google.maps.Point(30, 30)};");
        html.append("  bounds.extend(").append(location).append(");");
        html.append("  marker = new google.maps.Marker({");
        html.append("    position: ").append(location).append(",");
        html.append("    map: map,");
        html.append("    icon: image,");
        html.append("    optimized: false,");
        html.append("    title: \"").append(photo.getTitle()).append("\"});");
        html.append("  google.maps.event.addListener(marker, 'click', function() { java.open(").append(photo.getId()).append("); });");
      }
    }

    html.append("  map.fitBounds(bounds);");
    html.append("}");
    html.append("</script>");
    html.append("</head>");
    html.append("<body onload=\"initialise()\">");
    html.append("<div id=\"map_canvas\"></div>");
    html.append("</body>");
    html.append("</html>");
    return html.toString();
  }

  /**
   * Calculates size of the map marker based on the photo's width and height to avoid stretching the photo.
   *
   * @param photo the photo
   * @return string with width and height separated by a comma
   */
  private String getMarkerSize(Photo photo) {
    if (photo.getWidth().equals(photo.getHeight())) {
      return "60, 60";
    } else if (photo.getWidth() > photo.getHeight()) {
      double height = 60 * ((double) photo.getHeight() / (double) photo.getWidth());
      return "60," + height;
    } else {
      int width = 60 * (photo.getWidth() / photo.getHeight());
      return width + ", 60";
    }
  }

  /**
   * Method that gets Gooogle Maps Javascript API key
   *
   * @return returns a string with the API key
   */
  private String getApiKey() {
    try (InputStream input = new FileInputStream("config.properties")) {
      Properties prop = new Properties();
      prop.load(input);
      if (prop.getProperty("google_maps_api_key") == null) {
        return "";
      } else {
        return "&key=" + prop.getProperty("google_maps_api_key");
      }
    } catch (IOException ex) {
      FileLogger.getLogger().log(Level.FINE, ex.getMessage());
      FileLogger.closeHandler();
      return "";
    }
  }
}
