package com.lys.fastssm.base;

import java.beans.PropertyEditorSupport;
import java.lang.reflect.ParameterizedType;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSONObject;
import com.lys.fastssm.mybatis.PageCondition;
import com.lys.fastssm.mybatis.PageQuery;
import com.lys.fastssm.util.ControllerUtils;
import com.lys.fastssm.util.DateUtils;
import com.lys.fastssm.util.StringUtils;

/**
 * controller基础类
 * 定义了一些常用的单表增删改查跳转页面等功能
 * @author LYS
 */
public abstract class BaseController<T> {

	@SuppressWarnings("unchecked")
	public Class<T> getTargetType(){
		return (Class<T>)((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
	}
	
	public String getTargetName() {
		String name = this.getTargetService().getEntityClass().getName();
		return name.substring(0, 1).toLowerCase() + name.substring(1);
	}

	/**
	 * 获取目标service
	 * @return
	 */
	public abstract BaseService<T> getTargetService();
	
	@InitBinder
    public void initBinder(ServletRequestDataBinder binder) {
		//自动转换日期类型的字段格式
		binder.registerCustomEditor(Date.class, new PropertyEditorSupport() {
        	@Override
        	public void setAsText(String text) throws IllegalArgumentException {
        		if(StringUtils.isEmpty(text)) {
        			super.setValue(null);
        		} else {
        			Date value = DateUtils.parse(text);
        			super.setValue(value);
        		}
        	}
        	
        	@Override
        	public String getAsText() {
        		Object value = super.getValue();
        		if(value == null) {
        			return "";
        		}
        		Date date = (Date)value;
        		return DateUtils.format(date, "yyyy-MM-dd HH:mm:ss");
        	}
        });
	}
	
	/**
	 * 获取列表页url
	 * @return 返回url
	 */
	public String getListPageUrl() {
		String targetName = this.getTargetName();
		return targetName + "/" + targetName;
	}
	
	/**
	 * 显示列表页
	 * @param request 请求对象
	 * @return 返回 view
	 */
	@RequestMapping("/showList")
	public String showList(HttpServletRequest request){
		return this.getListPageUrl();
	}
	
	/**
	 * 获取列表
	 * @param request 请求对象
	 * @return 返回json结果
	 */
	@RequestMapping("/list")
	@ResponseBody
	public Object list(HttpServletRequest request) {
		PageQuery pageQuery = new PageQuery();
		//pageNo
		String pageNo = request.getParameter("pageNo");
		if(!StringUtils.isEmpty(pageNo)) {
			pageQuery.setPageNo(Integer.parseInt(pageNo));
		}
		//pageSize
		String pageSize = request.getParameter("pageSize");
		if(!StringUtils.isEmpty(pageSize)) {
			pageQuery.setPageSize(Integer.parseInt(pageSize));
		}
		//orderBy
		String orderBy = request.getParameter("orderBy");
		if(!StringUtils.isEmpty(orderBy)) {
			pageQuery.setOrderBy(orderBy);
		}
		//conditions
		String conditions = request.getParameter("conditions");
		if(!StringUtils.isEmpty(conditions)) {
			List<PageCondition> conditionList = JSONObject.parseArray(conditions, PageCondition.class);
			pageQuery.setConditions(conditionList);
		}
		//properties
		String properties = request.getParameter("properties");
		if(!StringUtils.isEmpty(properties)) {
			List<String> propertieList = JSONObject.parseArray(properties, String.class);
			String[] propertieArray = new String[propertieList.size()];
			propertieList.toArray(propertieArray);
			pageQuery.setProperties(propertieArray);
		}
		return this.getTargetService().select(pageQuery);
	}
	
	/**
	 * 获取添加页url
	 * @return 返回url
	 */
	public String getAddPageUrl() {
		String targetName = this.getTargetName();
		return targetName + "/" + targetName + "Add";
	}
	
	/**
	 * 显示添加页
	 * @param request 请求对象 
	 * @return 返回view
	 */
	@RequestMapping("/addPage")
	public String showAddPage(HttpServletRequest request){
		return this.getAddPageUrl();
	}
	
	/**
	 * 保存
	 * @param request 请求对象
	 * @param entity 实体
	 * @return 返回结果
	 */
	@RequestMapping("/save")
	@ResponseBody
	public Object save(HttpServletRequest request, T entity){
		this.getTargetService().insertSelective(entity);
		return ControllerUtils.returnSuccess(entity, "添加成功!");
	}
	
	/**
	 * 获取编辑页url
	 * @return 返回url
	 */
	public String getEditPageUrl() {
		String targetName = this.getTargetName();
		return targetName + "/" + targetName + "Edit";
	}
	
	/**
	 * 显示编辑页
	 * @param request 请求对象
	 * @param id id
	 * @return 返回 view
	 */
	@RequestMapping("/editPage")
	public String showEditPage(HttpServletRequest request, Long id){
		request.setAttribute("id", id);
		T entity = this.getTargetService().selectByPrimaryKey(id);
		request.setAttribute("entity", entity);
		return getEditPageUrl();
	}
	
	/**
	 * 更新
	 * @param request 请求对象
	 * @param entity 实体
	 * @return 返回结果
	 */
	@RequestMapping("/update")
	@ResponseBody
	public Object update(HttpServletRequest request, T entity) {
		if(this.getTargetService().updateSelective(entity) > 0) {
			return ControllerUtils.returnSuccess(entity, "保存成功!");
		}else {
			return ControllerUtils.returnFail("保存失败!");
		}
	}
	
	/**
	 * 删除
	 * @param request 请求对象
	 * @return 返回结果
	 */
	@RequestMapping("/delete")
	@ResponseBody
	public Object delete(HttpServletRequest request) {
		String ids = request.getParameter("id");
		if(StringUtils.isEmpty(ids)) {
			ids = request.getParameter("ids");
		}
		if(StringUtils.isEmpty(ids)) {
			return ControllerUtils.returnFail("请选择需要删除的记录!");
		}
		List<Long> idArray = JSONObject.parseArray(ids, Long.class);
		int count = 0;
		for(Long id : idArray) {
			count += this.getTargetService().deleteByPrimaryKey(id);
		}
		if(count > 0) {
			return ControllerUtils.returnSuccess(count, "删除成功!");
		}else {
			return ControllerUtils.returnFail("删除失败!");
		}
	}
}
