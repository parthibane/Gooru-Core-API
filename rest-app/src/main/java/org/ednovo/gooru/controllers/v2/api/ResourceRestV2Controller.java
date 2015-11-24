/////////////////////////////////////////////////////////////
//ResourceRestV2Controller.java
//rest-v2-app
// Created by Gooru on 2014
// Copyright (c) 2014 Gooru. All rights reserved.
// http://www.goorulearning.org/
// Permission is hereby granted, free of charge, to any person      obtaining
// a copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to
// permit persons to whom the Software is furnished to do so,  subject to
// the following conditions:
// The above copyright notice and this permission notice shall be
// included in all copies or substantial portions of the Software.
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY  KIND,
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE    WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR  PURPOSE     AND
// NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR  COPYRIGHT HOLDERS BE
// LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
// OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
// WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
/////////////////////////////////////////////////////////////
package org.ednovo.gooru.controllers.v2.api;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.ArrayUtils;
import org.ednovo.gooru.controllers.BaseController;
import org.ednovo.gooru.core.api.model.ActionResponseDTO;
import org.ednovo.gooru.core.api.model.Resource;
import org.ednovo.gooru.core.api.model.SessionContextSupport;
import org.ednovo.gooru.core.api.model.User;
import org.ednovo.gooru.core.constant.ConstantProperties;
import org.ednovo.gooru.core.constant.Constants;
import org.ednovo.gooru.core.constant.GooruOperationConstants;
import org.ednovo.gooru.core.constant.ParameterProperties;
import org.ednovo.gooru.core.security.AuthorizeOperations;
import org.ednovo.gooru.domain.service.CollectionService;
import org.ednovo.gooru.domain.service.resource.ResourceImportService;
import org.ednovo.gooru.domain.service.resource.ResourceService;
import org.ednovo.goorucore.application.serializer.JsonDeserializer;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.fasterxml.jackson.core.type.TypeReference;

@Controller
@RequestMapping(value = { "/v2/resource" })
public class ResourceRestV2Controller extends BaseController implements ConstantProperties, ParameterProperties {

	@Autowired
	private ResourceService resourceService;

	@Autowired
	private CollectionService collectionService;

	@Autowired
	private ResourceImportService resourceImportService;
	
	@AuthorizeOperations(operations = { GooruOperationConstants.OPERATION_RESOURCE_ADD })
	@RequestMapping(method = RequestMethod.POST, value = "")
	public ModelAndView createResource(@RequestBody final String data, final HttpServletRequest request, final HttpServletResponse response) throws Exception {
		request.setAttribute(PREDICATE, RESOURCE_CREATE_RESOURCE);
		final JSONObject json = requestData(data);
		final User user = (User) request.getAttribute(Constants.USER);
		final Resource resource = this.getResourceService().createResource(this.getResourceService().buildResourceFromInputParameters(getValue(RESOURCE, json), user), null, user, false);
		final String includes[] = (String[]) ArrayUtils.addAll(RESOURCE_INCLUDE_FIELDS, ERROR_INCLUDE);
		return toModelAndViewWithIoFilter(resource, RESPONSE_FORMAT_JSON, EXCLUDE_ALL, includes);
	}

	@AuthorizeOperations(operations = { GooruOperationConstants.OPERATION_RESOURCE_UPDATE })
	@RequestMapping(method = RequestMethod.PUT, value = "/{id}")
	public ModelAndView updateResource(@RequestBody final String data, @PathVariable(value = ID) final String resourceId, final HttpServletRequest request, final HttpServletResponse response) throws Exception {
		request.setAttribute(PREDICATE, RES_UPDATE_RES);
		final User user = (User) request.getAttribute(Constants.USER);
		final JSONObject json = requestData(data);
		final ActionResponseDTO<Resource> responseDTO = this.getResourceService().updateResource(resourceId, this.buildResourceFromInputParameters(getValue(RESOURCE, json)), getValue(RESOURCE_TAGS, json) == null ? null : buildResourceTags(getValue(RESOURCE_TAGS, json)), user);
		if (responseDTO.getErrors().getErrorCount() > _ZERO) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		}
		String[] includeFields = getValue(FIELDS, json) != null ? getFields(getValue(FIELDS, json)) : null;
		final String includes[] = (String[]) ArrayUtils.addAll(includeFields == null ? RESOURCE_INCLUDE_FIELDS : includeFields, ERROR_INCLUDE);

		return toModelAndViewWithIoFilter(responseDTO.getModelData(), RESPONSE_FORMAT_JSON, EXCLUDE_ALL, includes);
	}

	@AuthorizeOperations(operations = { GooruOperationConstants.OPERATION_RESOURCE_READ })
	@RequestMapping(method = RequestMethod.GET, value = "/{id}")
	public ModelAndView getResource(final HttpServletRequest request, @PathVariable(ID) final String resourceId, final HttpServletResponse response) throws Exception {
		return toModelAndViewWithIoFilter(this.getResourceService().getResource(resourceId), RESPONSE_FORMAT_JSON, EXCLUDE_ALL, true, RESOURCE_INCLUDE_FIELDS);
	}

	@AuthorizeOperations(operations = { GooruOperationConstants.OPERATION_RESOURCE_READ })
	@RequestMapping(method = RequestMethod.GET, value = "/collection/resource/list")
	public ModelAndView listResourcesUsedInCollections(@RequestParam(value = DATA_OBJECT, required = true) String data, HttpServletRequest request, HttpServletResponse response) throws Exception {
		final User user = (User) request.getAttribute(Constants.USER);
		final JSONObject json = requestData(data);
		return toModelAndView(serialize(this.getResourceService().listResourcesUsedInCollections(getValue(LIMIT_FIELD, json), getValue(OFFSET_FIELD, json), user), RESPONSE_FORMAT_JSON));
	}

	@AuthorizeOperations(operations = { GooruOperationConstants.OPERATION_RESOURCE_DELETE })
	@RequestMapping(method = RequestMethod.DELETE, value = "/{id}")
	public void deleteResource(@PathVariable(value = ID) final String resourceId, final HttpServletRequest request, final HttpServletResponse response) throws Exception {
		request.setAttribute(PREDICATE, RESOURCE_DELETE_RESOURCE);
		final User user = (User) request.getAttribute(Constants.USER);
		this.resourceService.deleteResource(resourceId, user);
		response.setStatus(HttpServletResponse.SC_NO_CONTENT);
	}

	@AuthorizeOperations(operations = { GooruOperationConstants.OPERATION_RESOURCE_UPDATE })
	@RequestMapping(value = "/collaborators/{id}", method = { RequestMethod.PUT })
	public ModelAndView addCollborators(@PathVariable(value = ID) final String collectionId, final HttpServletRequest request, final HttpServletResponse response, @RequestParam(value = "collaborator", required = true) final String collaboratorId) {
		final User user = (User) request.getAttribute(Constants.USER);

		// To capture activity log
		SessionContextSupport.putLogParameter(EVENT_NAME, "add-collaborators");
		SessionContextSupport.putLogParameter(GOORU_UID, user.getPartyUid());
		SessionContextSupport.putLogParameter(COLLECTION_ID, collectionId);
		return toModelAndViewWithIoFilter(this.getResourceService().addCollaborator(collectionId, user, collaboratorId, ADD), RESPONSE_FORMAT_JSON, EXCLUDE_ALL, COLLABORATORI_INCLUDE);
	}

	@AuthorizeOperations(operations = { GooruOperationConstants.OPERATION_RESOURCE_READ })
	@RequestMapping(value = "/collaborators/{id}", method = RequestMethod.GET)
	public ModelAndView getCollaborators(@PathVariable(value = ID) final String collectionId, final HttpServletRequest request, final HttpServletResponse response) {

		// To capture activity log
		SessionContextSupport.putLogParameter(EVENT_NAME, "get-collaborators");
		SessionContextSupport.putLogParameter(COLLECTION_ID, collectionId);
		return toModelAndViewWithIoFilter(this.getCollectionService().getCollaborators(collectionId), RESPONSE_FORMAT_JSON, EXCLUDE_ALL, COLLABORATORI_INCLUDE);
	}

	@AuthorizeOperations(operations = { GooruOperationConstants.OPERATION_RESOURCE_DELETE })
	@RequestMapping(value = "/collaborators/{id}", method = RequestMethod.DELETE)
	public ModelAndView deleteCollaborators(@PathVariable(value = ID) final String collectionId, final HttpServletRequest request, final HttpServletResponse response, @RequestParam(value = "collaborator", required = true) final String collaboratorId) {
		final User user = (User) request.getAttribute(Constants.USER);

		// To capture activity log
		SessionContextSupport.putLogParameter(EVENT_NAME, "delete-collaborators");
		SessionContextSupport.putLogParameter(COLLECTION_ID, collectionId);
		SessionContextSupport.putLogParameter(GOORU_UID, user.getPartyUid());

		return toModelAndViewWithIoFilter(this.getResourceService().addCollaborator(collectionId, user, collaboratorId, DELETE), RESPONSE_FORMAT_JSON, EXCLUDE_ALL, COLLABORATORI_INCLUDE);
	}

	@AuthorizeOperations(operations = { GooruOperationConstants.OPERATION_RESOURCE_DELETE })
	@RequestMapping(method = { RequestMethod.DELETE, RequestMethod.PUT }, value = "/{id}/taxonomy")
	public void deleteTaxonomyResource(@RequestBody final String data, @PathVariable(value = ID) final String resourceId, final HttpServletRequest request, final HttpServletResponse response) throws Exception {
		request.setAttribute(PREDICATE, RESOURCE_DELETE_RESOURCE_TAXONOMY);
		final User user = (User) request.getAttribute(Constants.USER);
		final JSONObject json = requestData(data);
		this.getResourceService().deleteTaxonomyResource(resourceId, this.buildResourceFromInputParameters(getValue(RESOURCE, json)), user);

		SessionContextSupport.putLogParameter(EVENT_NAME, "taxonomy-resource-delete");
		SessionContextSupport.putLogParameter(GOORU_OID, resourceId);
		SessionContextSupport.putLogParameter(USER_ID, user.getUserId());
		SessionContextSupport.putLogParameter(GOORU_UID, user.getPartyUid());

		response.setStatus(HttpServletResponse.SC_NO_CONTENT);
	}

	@AuthorizeOperations(operations = { GooruOperationConstants.OPERATION_RESOURCE_READ })
	@RequestMapping(method = RequestMethod.GET, value = "/suggest/meta/info")
	public ModelAndView suggestResourceMetaData(@RequestParam(value = URL) final String url, @RequestParam(value = TITLE, required = false) final String title, @RequestParam(value = FETCH_THUMBNAIL, required = false, defaultValue = "false") final boolean fetchThumbnail,
			final HttpServletRequest request, final HttpServletResponse response) throws Exception {
		return toModelAndView(serializeToJson(getResourceService().getSuggestedResourceMetaData(url, title, fetchThumbnail), true));
	}

	@AuthorizeOperations(operations = { GooruOperationConstants.OPERATION_RESOURCE_UPDATE })
	@RequestMapping(method = RequestMethod.POST, value = "/{id}/media")
	public ModelAndView updateResourceImage(final HttpServletRequest request, @PathVariable(ID) final String resourceId, @RequestBody final String data, final HttpServletResponse response) throws Exception {
		final JSONObject json = requestData(data);
		return toModelAndView(serializeToJson(this.getResourceService().updateResourceImage(resourceId, getValue(FILENAME, json)), true));
	}

	@AuthorizeOperations(operations = { GooruOperationConstants.OPERATION_RESOURCE_READ })
	@RequestMapping(method = RequestMethod.GET, value = { "/{id}/play" })
	public ModelAndView getResourceSource(final HttpServletRequest request, @PathVariable(value = ID) final String gooruContentId, final HttpServletResponse response, @RequestParam(value = INCLUDE_BROKEN_PDF, required = false, defaultValue = TRUE) final Boolean includeBrokenPdf,
			@RequestParam(value = MORE, required = false, defaultValue = TRUE) final boolean more) throws Exception {
		request.setAttribute(PREDICATE, RESOURCE_SRC_GET);
		final User apiCaller = (User) request.getAttribute(Constants.USER);
		return toModelAndView(serialize(this.getResourceService().resourcePlay(gooruContentId, apiCaller, more), RESPONSE_FORMAT_JSON, EXCLUDE_ALL, true, RESOURCE_INCLUDE_FIELDS));

	}

	@AuthorizeOperations(operations = { GooruOperationConstants.OPERATION_RESOURCE_DELETE })
	@RequestMapping(method = RequestMethod.DELETE, value = "content/{id}")
	public void deleteContentProvider(@PathVariable(value = ID) final String gooruOid, @RequestParam(value = "providerType") final String providerType, @RequestParam(value = "name") final String name, final HttpServletRequest request, final HttpServletResponse response) throws Exception {
		this.getResourceService().deleteContentProvider(gooruOid, providerType, name);
	}

	@AuthorizeOperations(operations = { GooruOperationConstants.OPERATION_RESOURCE_READ })
	@RequestMapping(method = RequestMethod.GET, value = "/url/exist")
	public ModelAndView checkResourceUrlExists(@RequestParam(value = URL) final String url, @RequestParam(required = false, value = CHK_SHORTENED_URL, defaultValue = FALSE) final boolean checkShortenedUrl) throws Exception {
		return toModelAndViewWithIoFilter(this.getResourceService().checkResourceUrlExists(url, checkShortenedUrl), RESPONSE_FORMAT_JSON, EXCLUDE_ALL, true, RESOURCE_INSTANCE_INCLUDES);
	}
	
	@AuthorizeOperations(operations = { GooruOperationConstants.OPERATION_RESOURCE_READ })
	@RequestMapping(method = RequestMethod.POST, value = "/url/exist")
	public ModelAndView resourceUrlExists(@RequestParam(value = URL) final String url, @RequestParam(required = false, value = CHK_SHORTENED_URL, defaultValue = FALSE) final boolean checkShortenedUrl) throws Exception {
		return toModelAndViewWithIoFilter(this.getResourceService().checkResourceUrlExists(url, checkShortenedUrl), RESPONSE_FORMAT_JSON, EXCLUDE_ALL, true, RESOURCE_INSTANCE_INCLUDES);
	}

	@AuthorizeOperations(operations = { GooruOperationConstants.OPERATION_USER_READ })
	@RequestMapping(method = { RequestMethod.GET }, value = "/{id}/collection")
	public ModelAndView getUserListByResourceId(@PathVariable(value = ID) final String resourceId, @RequestParam(value = SHARING, required = false ,defaultValue = PUBLIC) final String sharing, @RequestParam(value = OFFSET_FIELD, required = false, defaultValue = "0") final Integer offset, @RequestParam(value = LIMIT_FIELD, required = false, defaultValue = "20") final Integer limit,
			final HttpServletRequest request, final HttpServletResponse response) {
		return toModelAndView(serialize(this.getResourceService().getCollectionsByResourceId(resourceId, sharing, limit, offset), RESPONSE_FORMAT_JSON, EXCLUDE_ALL, true, true, RESOURCE_COLLECTION_USED_INCLUDE_FIELDS));
	}

	@AuthorizeOperations(operations = { GooruOperationConstants.OPERATION_RESOURCE_ADD })
	@RequestMapping(method = RequestMethod.POST, value = "{type}/import")
	public void createResource(@PathVariable(value = TYPE) final String type, @RequestParam(value = FILENAME) final String filename, final HttpServletRequest request, final HttpServletResponse response) throws Exception {
		final User user = (User) request.getAttribute(Constants.USER);
		this.getResourceImportService().createOrUpdateResource(filename, user);
	}
	
	private Resource buildResourceFromInputParameters(final String data) {
		return JsonDeserializer.deserialize(data, Resource.class);
	}

	private List<String> buildResourceTags(final String data) {
		return JsonDeserializer.deserialize(data, new TypeReference<List<String>>() {
		});
	}

	public ResourceService getResourceService() {
		return resourceService;
	}

	public CollectionService getCollectionService() {
		return collectionService;
	}

	public ResourceImportService getResourceImportService() {
		return resourceImportService;
	}
}
