package org.ednovo.gooru.controllers.v1.api;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.ArrayUtils;
import org.ednovo.gooru.controllers.BaseController;
import org.ednovo.gooru.core.api.model.ActionResponseDTO;
import org.ednovo.gooru.core.api.model.Collection;
import org.ednovo.gooru.core.api.model.Job;
import org.ednovo.gooru.core.api.model.RequestMappingUri;
import org.ednovo.gooru.core.api.model.User;
import org.ednovo.gooru.core.constant.ConstantProperties;
import org.ednovo.gooru.core.constant.Constants;
import org.ednovo.gooru.core.constant.GooruOperationConstants;
import org.ednovo.gooru.core.security.AuthorizeOperations;
import org.ednovo.gooru.domain.component.CollectionCopyProcessor;
import org.ednovo.gooru.domain.component.CollectionDeleteProcessor;
import org.ednovo.gooru.domain.service.ClassService;
import org.ednovo.gooru.domain.service.collection.CourseService;
import org.ednovo.goorucore.application.serializer.JsonDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.ednovo.gooru.application.spring.ClearCache;
import org.ednovo.gooru.application.spring.RedisCache;

@RequestMapping(value = { RequestMappingUri.COURSE })
@Controller
public class CourseRestController extends BaseController implements ConstantProperties {

	@Autowired
	private CourseService courseService;

	@Autowired
	private ClassService classService;

	@Autowired
	private CollectionCopyProcessor collectionCopyProcessor;
	
	@Autowired
	private CollectionDeleteProcessor collectionDeleteProcessor;

	
	@AuthorizeOperations(operations = { GooruOperationConstants.OPERATION_SCOLLECTION_ADD })
	@RequestMapping(method = RequestMethod.POST)
	public ModelAndView createCourse(@RequestBody final String data, final HttpServletRequest request, final HttpServletResponse response) {
		final User user = (User) request.getAttribute(Constants.USER);
		final ActionResponseDTO<Collection> responseDTO = this.getCourseService().createCourse(buildCourse(data), user);
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
	@ClearCache(key = {CONTENT}, id = ID)
	@RequestMapping(value = RequestMappingUri.ID, method = RequestMethod.PUT)
	public void updateCourse(@PathVariable(value = ID) final String courseId, @RequestBody final String data, final HttpServletRequest request, final HttpServletResponse response) {
		final User user = (User) request.getAttribute(Constants.USER);
		this.getCourseService().updateCourse(courseId, buildCourse(data), user);
	}

	@AuthorizeOperations(operations = { GooruOperationConstants.OPERATION_SCOLLECTION_READ })
	@RedisCache(key = {CONTENT}, ttl=EXPIRY)
	@RequestMapping(value = RequestMappingUri.ID, method = RequestMethod.GET)
	public ModelAndView getCourse(@PathVariable(value = ID) final String courseId, final HttpServletRequest request, final HttpServletResponse response) {
		return toModelAndViewWithIoFilter(this.getCourseService().getCourse(courseId), RESPONSE_FORMAT_JSON, EXCLUDE_ALL, true, "*");
	}

	@AuthorizeOperations(operations = { GooruOperationConstants.OPERATION_SCOLLECTION_READ })
	@RedisCache(key = {CONTENT,COURSES}, ttl=EXPIRY)
	@RequestMapping(method = RequestMethod.GET)
	public ModelAndView getCourses(@RequestParam(value = OFFSET_FIELD, required = false, defaultValue = "0") int offset, @RequestParam(value = LIMIT_FIELD, required = false, defaultValue = "10") int limit, final HttpServletRequest request, final HttpServletResponse response) throws Exception {
		return toModelAndViewWithIoFilter(this.getCourseService().getCourses(limit, offset), RESPONSE_FORMAT_JSON, EXCLUDE_ALL, true, "*");
	}

	@AuthorizeOperations(operations = { GooruOperationConstants.OPERATION_CLASSPAGE_READ })
	@RedisCache(key = {CONTENT}, ttl=EXPIRY)
	@RequestMapping(value = RequestMappingUri.COURSES_CLASS, method = RequestMethod.GET)
	public ModelAndView getClasses(@PathVariable(value = ID) final String courseGooruOid, @RequestParam(value = OFFSET_FIELD, required = false, defaultValue = "0") final int offset, @RequestParam(value = LIMIT_FIELD, required = false, defaultValue = "10") final int limit,
			final HttpServletRequest request, final HttpServletResponse response) {
		return toModelAndViewWithIoFilter(this.getClassService().getClassesByCourse(courseGooruOid, limit, offset), RESPONSE_FORMAT_JSON, EXCLUDE_ALL, true, CLASS_INCLUDES);
	}

	@AuthorizeOperations(operations = { GooruOperationConstants.OPERATION_SCOLLECTION_READ })
	@ClearCache(key = {CONTENT}, id = ID)
	@RequestMapping(value = RequestMappingUri.ID, method = RequestMethod.DELETE)
	public void deleteCourse(@PathVariable(value = ID) final String courseId, final HttpServletRequest request, final HttpServletResponse response) {
		final User user = (User) request.getAttribute(Constants.USER);
		this.getCourseService().deleteCourse(courseId, user);
		getCollectionDeleteProcessor().deleteContent(courseId, COURSE);
	}

	@AuthorizeOperations(operations = { GooruOperationConstants.OPERATION_SCOLLECTION_COPY })
	@ClearCache(key = {CONTENT}, id = ID)
	@RequestMapping(value = RequestMappingUri.ID, method = RequestMethod.POST)
	public ModelAndView copyCourse(@PathVariable(value = ID) final String courseId, final HttpServletRequest request, final HttpServletResponse response) {
		final User user = (User) request.getAttribute(Constants.USER);
		Job job = getCollectionCopyProcessor().copyCourse(courseId, user);
		String includes[] = (String[]) ArrayUtils.addAll(CREATE_INCLUDES, ERROR_INCLUDE);
		job.setUri(buildUri(RequestMappingUri.V2_JOB, String.valueOf(job.getJobId())));
		return toModelAndViewWithIoFilter(job, RESPONSE_FORMAT_JSON, EXCLUDE_ALL, true, includes);
	}

	private Collection buildCourse(final String data) {
		return JsonDeserializer.deserialize(data, Collection.class);
	}

	public CourseService getCourseService() {
		return courseService;
	}

	public ClassService getClassService() {
		return classService;
	}

	public CollectionCopyProcessor getCollectionCopyProcessor() {
		return collectionCopyProcessor;
	}

	public CollectionDeleteProcessor getCollectionDeleteProcessor() {
		return collectionDeleteProcessor;
	}


}
