package net.bytle.tower.eraldy.objectProvider;

import net.bytle.exception.NotFoundException;
import net.bytle.tower.ApiClient;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.type.UriEnhanced;

public class ApiClientProvider {
  @SuppressWarnings({"FieldCanBeLocal", "unused"})
  private final EraldyApiApp apiApp;
  private ApiClient interactApiClient;

  public ApiClientProvider(EraldyApiApp eraldyApiApp) {
    this.apiApp = eraldyApiApp;
  }

  public ApiClient getClientFromRedirectUri(UriEnhanced redirectUriEnhanced) throws NotFoundException {

    UriEnhanced interactAppUri = UriEnhanced.createFromUri(this.interactApiClient.getUri());

    if (redirectUriEnhanced.getHostWithPort().equals(interactAppUri.getHostWithPort())) {
      return this.interactApiClient;
    }
    throw new NotFoundException("The client api could not found for the redirect URI (" + redirectUriEnhanced + ")");

  }

  public void setInteractAppClient(ApiClient apiClient) {
    this.interactApiClient = apiClient;
  }

}
