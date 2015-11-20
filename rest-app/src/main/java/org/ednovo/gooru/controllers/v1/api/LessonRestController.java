package org.ednovo.gooru.controllers.v1.api;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.ArrayUtils;
import org.ednovo.gooru.application.spring.ClearCache;
import org.ednovo.gooru.application.spring.RedisCache;
import org.ednovo.gooru.controllers.BaseController;
import org.ednovo.gooru.core.api.model.ActionResponseDTO;
import org.ednovo.gooru.core.api.model.Collection;
import org.ednovo.gooru.core.api.model.Job;
import org.ednovo.gooru.core.api.model.RequestMappingUri;
import org.ednovo.gooru.core.api.model.User;
import org.ednovo.gooru.core.constant.ConstantProperties;
import org.ednovo.gooru.core.constant.Constants;
import org.ednovo.gooru.core.constant.GooruOperationConstants;
import org.ednovo.gooru.core.constant.ParameterProperties;
import org.ednovo.gooru.core.security.AuthorizeOperations;
import org.ednovo.gooru.domain.component.CollectionCopyProcessor;
import org.ednovo.gooru.domain.component.CollectionDeleteProcessor;
import org.ednovo.gooru.domain.service.collection.LessonService;
import org.ednovo.goorucore.application.serializer.JsonDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@RequestMapping(value = { RequestMappingUri.LESSON })
@Controller
public class LessonRestController extends BaseController implements ConstantProperties, ParameterProperties {

	@Autowired
	private LessonService lessonService;
	
	@Autowired
	private CollectionCopyProcessor collectionCopyProcessor;
	
	@Autowired
	private CollectionDeleteProcessor collectionDeleteProcessor;

	@AuthorizeOperations(operations = { GooruOperationConstants.OPERATION_SCOLLECTION_ADD })
	@ClearCache(key = {CONTENT}, id = UNIT_ID)
	@RequestMapping(method = RequestMethod.POST)
	public ModelAndView createLesson(@PathVariable(value = COURSE_ID) final String courseId, @PathVariable(value = UNIT_ID) final String unitId, @RequestBody final String data, final HttpServletRequest request, final HttpServletResponse response) {
		final User user = (User) request.getAttribute(Constants.USER);
		final ActionResponseDTO<Collection> responseDTO = this.getLessonService().createLesson(courseId, unitId, buildLesson(data), user);
		if (responseDTO.getErrors().getErrorCount() > 0) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		} else {
			response.setStatus(HttpServletResponse.SC_CREATED);
			responseDTO.getModel().setUri(generateUri(request.getRequestURI(), responseDTO.getModel().getGooruOid()));
		}
		String includes[] = (String[]) ArrayUtils.addAll(CREATE_INCLUDES, ERROR_INCLUDE);
		return toModelAndViewWithIoFilter(responseDTO.getModelData(), RESPONSE_FORMAT_JSON, EXCLUDE_ALL, true, includes);
	}

	@AuthorizeOperations(operations = { GooruOperationConstants.OPERATION_SCOLLECTION_UPDATE })
	@ClearCache(key = {CONTENT}, id = UNIT_ID)
	@RequestMapping(value = RequestMappingUri.ID, method = RequestMethod.PUT)
	public void updateLesson(@PathVariable(value = COURSE_ID) final String courseId, @PathVariable(value = UNIT_ID) final String unitId, @PathVariable(value = ID) final String lessonId, @RequestBody final String data, final HttpServletRequest request, final HttpServletResponse response) {
		final User user = (User) request.getAttribute(Constants.USER);
		this.getLessonService().updateLesson(courseId, unitId, lessonId, buildLesson(data), user);
	}

	@AuthorizeOperations(operations = { GooruOperationConstants.OPERATION_SCOLLECTION_READ })
	@RedisCache(key = {CONTENT}, ttl=EXPIRY)
	@RequestMapping(value = RequestMappingUri.ID, method = RequestMethod.GET)
	public ModelAndView getLesson(@PathVariable(value = COURSE_ID) final String courseId, @PathVariable(value = UNIT_ID) final String unitId, @PathVariable(value = ID) final String lessonId, final HttpServletRequest request, final HttpServletResponse response) {
		return toModelAndViewWithIoFilter(this.getLessonService().getLesson(lessonId), RESPONSE_FORMAT_JSON, EXCLUDE_ALL, true, "*");
	}

	@AuthorizeOperations(operations = { GooruOperationConstants.OPERATION_SCOLLECTION_READ })
	@RedisCache(key = {CONTENT,LESSONS}, ttl=EXPIRY)
	@RequestMapping(method = RequestMethod.GET)
	public ModelAndView getLessons(@PathVariable(value = COURSE_ID) final String courseId, @PathVariable(value = UNIT_ID) final String unitId, @RequestParam(value = OFFSET_FIELD, required = false, defaultValue = "0") int offset, @RequestParam(value = LIMIT_FIELD, required = false, defaultValue = "10") int limit, final HttpServletRequest request, final HttpServletResponse response) {
		return toModelAndViewWithIoFilter(this.getLessonService().getLessons(unitId, limit, offset), RESPONSE_FORMAT_JSON, EXCLUDE_ALL, true, "*");
	}

	@AuthorizeOperations(operations = { GooruOperationConstants.OPERATION_SCOLLECTION_DELETE })
	@ClearCache(key = {CONTENT}, id = UNIT_ID)
	@RequestMapping(value = RequestMappingUri.ID, method = RequestMethod.DELETE)
	public void deleteLesson(@PathVariable(value = COURSE_ID) final String courseId, @PathVariable(value = UNIT_ID) final String unitId, @PathVariable(value = ID) final String lessonId, final HttpServletRequest request, final HttpServletResponse response) {
		final User user = (User) request.getAttribute(Constants.USER);
		this.getLessonService().deleteLesson(courseId, unitId, lessonId, user);
		getCollectionDeleteProcessor().deleteContent(lessonId, LESSON);
	}
	
	@AuthorizeOperations(operations = { GooruOperationConstants.OPERATION_SCOLLECTION_COPY })
	@ClearCache(key = {CONTENT}, id = UNIT_ID)
	@RequestMapping(value = RequestMappingUri.ID, method = RequestMethod.POST)
	public ModelAndView copyLesson(@PathVariable(value = COURSE_ID) final String courseId, @PathVariable(value = UNIT_ID) final String unitId, @PathVariable(value = ID) final String lessonId, final HttpServletRequest request, final HttpServletResponse response) {
		final User user = (User) request.getAttribute(Constants.USER);
		Job job = getCollectionCopyProcessor().copyLesson(courseId, unitId, lessonId, user);
		String includes[] = (String[]) ArrayUtils.addAll(CREATE_INCLUDES, ERROR_INCLUDE);
		job.setUri(buildUri(RequestMappingUri.V2_JOB, String.valueOf(job.getJobId())));
		return toModelAndViewWithIoFilter(job, RESPONSE_FORMAT_JSON, EXCLUDE_ALL, true, includes);
	}

	private Collection buildLesson(final String data) {
		return JsonDeserializer.deserialize(data, Collection.class);
	}
	
	public LessonService getLessonService() {
		return lessonService;
	}

	public CollectionCopyProcessor getCollectionCopyProcessor() {
		return collectionCopyProcessor;
	}

	public CollectionDeleteProcessor getCollectionDeleteProcessor() {
		return collectionDeleteProcessor;
	}

}
