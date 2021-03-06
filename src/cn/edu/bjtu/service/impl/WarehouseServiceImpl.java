package cn.edu.bjtu.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import cn.edu.bjtu.bean.search.WarehouseSearchBean;
import cn.edu.bjtu.dao.WarehouseDao;
import cn.edu.bjtu.service.FocusService;
import cn.edu.bjtu.service.WarehouseService;
import cn.edu.bjtu.util.Constant;
import cn.edu.bjtu.util.IdCreator;
import cn.edu.bjtu.util.PageUtil;
import cn.edu.bjtu.util.UploadFile;
import cn.edu.bjtu.vo.Warehouse;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

@Repository
/**
 * 
 * @author RussWest0
 *
 */
@Transactional
public class WarehouseServiceImpl implements WarehouseService {

	@Resource
	WarehouseDao warehouseDao;
	@Resource
	Warehouse warehouse;
	@Autowired
	FocusService focusService;

	@Override
	public Warehouse getWarehouseInfo(String Warehouseid) {
		return warehouseDao.getWarehouseInfo(Warehouseid);
	}

	@Override
	public boolean insertNewWarehouse(Warehouse warehouse,HttpServletRequest request,MultipartFile file){
		String carrierId = (String) request.getSession().getAttribute(Constant.USER_ID);
		//保存文件
		String fileLocation=UploadFile.uploadFile(file, carrierId, "warehouse");

		warehouse.setId(IdCreator.createRepositoryId());
		warehouse.setCarrierId(carrierId);
		warehouse.setRelDate(new Date());
		//设置文件位置 
		warehouse.setDetailPrice(fileLocation);
		warehouseDao.save(warehouse);// 保存实体
		return true;
	}

	@Override
	public boolean updateNewWarehouse(Warehouse warehouse,HttpServletRequest request,MultipartFile file){
		String carrierId = (String) request.getSession().getAttribute(Constant.USER_ID);
		//保存文件
		String fileLocation=UploadFile.uploadFile(file, carrierId, "warehouse");

		Warehouse warehouseInstance = warehouseDao.get(Warehouse.class,warehouse.getId());
		warehouseInstance.setName(warehouse.getName());
		warehouseInstance.setCity(warehouse.getCity());
		warehouseInstance.setAddress(warehouse.getAddress());
		warehouseInstance.setType(warehouse.getType());
		warehouseInstance.setKind(warehouse.getKind());
		warehouseInstance.setHouseArea(warehouse.getHouseArea());
		warehouseInstance.setYardArea(warehouse.getYardArea());
		warehouseInstance.setHeight(warehouse.getHeight());
		warehouseInstance.setFireRate(warehouse.getFireRate());
		warehouseInstance.setStorageForm(warehouse.getStorageForm());
		warehouseInstance.setFireSecurity(warehouse.getFireSecurity());
		warehouseInstance.setEnvironment(warehouse.getEnvironment());
		warehouseInstance.setServiceContent(warehouse.getServiceContent());
		warehouseInstance.setContact(warehouse.getContact());
		warehouseInstance.setPhone(warehouse.getPhone());
		warehouseInstance.setRelDate(new Date());
		warehouseInstance.setRemarks(warehouse.getRemarks());
		warehouseInstance.setCarrierId(carrierId);
		
		//设置文件位置 
		warehouseInstance.setDetailPrice(fileLocation);

		//更新
		warehouseDao.update(warehouseInstance);
		return true;
	}
	
	public boolean deleteWarehouse(String id){
		warehouse = getWarehouseInfo(id);// 根据id查找到仓库信息
		warehouseDao.delete(warehouse);
		//设置关注信息为失效
		focusService.setInvalid(id);
		
		return true;
	}

	/**
	 * 资源栏-仓库筛选
	 */
	@Override
	public JSONArray getSelectedWarehouseNew(WarehouseSearchBean warehouseBean,
			PageUtil pageUtil, HttpSession session) {
		String userId=(String)session.getAttribute(Constant.USER_ID);
		Map<String,Object> params=new HashMap<String,Object>();
			String sql = "select t1.id,"
				+ "t1.carrierId,"
				+ "t1.name,"
				+ "t1.companyName,"
				+ "t1.fireRate,"
				+ "t1.type,"
				+ "t1.houseArea,"
				+ "t1.relDate,"
				+ "t3.status "
				+ " from warehouse_carrier_view t1 "
				+ "left join ("
				+ "select * from focus t2 ";
				
		if(userId!=null){//如果当前有用户登录在条件中加入用户信息
			sql+=" where t2.focusType='warehouse' and t2.clientId=:clientId ";
			params.put("clientId", userId);
		}else{
			sql+=" where t2.focusType='warehouse' and t2.clientId='' ";
		}
		sql+=") t3 on t1.id=t3.focusId ";
		String wheresql=whereSql(warehouseBean,params);
		sql+=wheresql+" order by t1.relDate desc";
		
		JSONArray jsonArray = new JSONArray();
		int page=pageUtil.getCurrentPage()==0?1:pageUtil.getCurrentPage();
		int display=pageUtil.getDisplay()==0?10:pageUtil.getDisplay();
		List<Object[]> objectList=warehouseDao.findBySql(sql, params,page,display);
		
		List<WarehouseSearchBean> warehouseList=new ArrayList<WarehouseSearchBean>();
		for(Iterator<Object[]> it=objectList.iterator();it.hasNext();){
			WarehouseSearchBean instanceBean=new WarehouseSearchBean();
			Object[] obj=it.next();
			instanceBean.setId((String)obj[0]);
			instanceBean.setCarrierId((String)obj[1]);
			instanceBean.setName((String)obj[2]);;
			instanceBean.setCompanyName((String)obj[3]);;
			instanceBean.setFireRate((String)obj[4]);
			instanceBean.setType((String)obj[5]);
			instanceBean.setHouseArea((Float)obj[6]+"");
			instanceBean.setRelDate((Date)obj[7]);;
			instanceBean.setStatus((String)obj[8]);
			warehouseList.add(instanceBean);
		}
		
		for(int i=0;i<warehouseList.size();i++){
			JSONObject jsonObject=(JSONObject)JSONObject.toJSON(warehouseList.get(i));
			jsonArray.add(jsonObject);
		}
		return jsonArray;
	}
	
	/**
	 * where sql
	 * @param warehouseBean
	 * @param params
	 * @return
	 */
	private String whereSql(WarehouseSearchBean warehouseBean,Map<String,Object> params){
		String wheresql=" where 1=1 ";
		if(warehouseBean.getCity()!=null && !warehouseBean.getCity().equals("中文或拼音") && !warehouseBean.getCity().equals("All") && !warehouseBean.getCity().equals("") && !warehouseBean.getCity().equals("全国")){
			wheresql+=" and t1.city=:city";
			params.put("city", warehouseBean.getCity());
		}
		if(warehouseBean.getType()!=null && !warehouseBean.getType().equals("") && !warehouseBean.getType().equals("All")&& !warehouseBean.getType().equals("")){
			String type=warehouseBean.getType();
			if(type.equals("保税仓库")){
				wheresql+=" and t1.type='保税'";
			}
			if(type.equals("非保税仓库")){
				wheresql+=" and t1.type='非保税'";
			}
		}
		if(warehouseBean.getStorageForm()!= null && !warehouseBean.getStorageForm().equals("") &&!warehouseBean.getStorageForm().equals("All")){
			String storageForm=warehouseBean.getStorageForm();
			if(storageForm.equals("普通仓库")){
				wheresql+=" and t1.storageForm='普通'";
			}
			if(storageForm.equals("冷藏仓库")){
				wheresql+=" and t1.storageForm='冷藏'";
			}
			if(storageForm.equals("恒温仓库")){
				wheresql+=" and t1.storageForm='恒温'";
			}
			if(storageForm.equals("露天仓库")){
				wheresql+=" and t1.storageForm='露天'";
			}
			if(storageForm.equals("危险品仓库")){
				wheresql+=" and t1.storageForm='危险品'";
			}
		}
		if(warehouseBean.getHouseArea()!=null && !warehouseBean.getHouseArea().equals("") && !warehouseBean.getHouseArea().equals("All")){
			String houseArea=warehouseBean.getHouseArea();
			if (houseArea.equals("大于1万平方米")) {
				wheresql+=" and t1.houseArea>=10000";
			}
			if (houseArea.equals("大于2万平方米")) {
				wheresql+=" and t1.houseArea>=20000";
			}
			if (houseArea.equals("大于5万平方米")) {
				wheresql+=" and t1.houseArea>=50000";
			}
		}
		
		return wheresql;
	}

	/**
	 * 资源栏-仓库筛选总记录数
	 */
	@Override
	public Integer getSelectedWarehouseTotalRows(
			WarehouseSearchBean warehouseBean) {
		Map<String,Object> params=new HashMap<String,Object>();
		String hql="select count(*) from WarehouseCarrierView t1 "+whereSql(warehouseBean, params);
		Long count=warehouseDao.count(hql, params);
		
		return count.intValue();
	}
	
	/**
	 * 我的信息-货物信息
	 */
	@Override
	public JSONArray getUserWarehouseResource(HttpSession session,PageUtil pageUtil) {
		
		String carrierId=(String)session.getAttribute(Constant.USER_ID);
		String hql="from Warehouse t where t.carrierId=:carrierId order by t.relDate desc ";
		Map<String,Object> params=new HashMap<String,Object>();
		params.put("carrierId", carrierId);
		int page=pageUtil.getCurrentPage()==0?1:pageUtil.getCurrentPage();
		int display=pageUtil.getDisplay()==0?10:pageUtil.getDisplay();
		List<Warehouse> warehouseList=warehouseDao.find(hql, params,page,display);
		JSONArray jsonArray=new JSONArray();
		for(Warehouse warehouse:warehouseList){
			JSONObject jsonObject=(JSONObject)JSONObject.toJSON(warehouse);
			jsonArray.add(jsonObject);
		}
		return jsonArray;
	}

	/**
	 * 我的信息-货物信息-总记录条数
	 */
	@Override
	public Integer getUserWarehouseResourceTotalRows(HttpSession session) {
		String carrierId=(String)session.getAttribute(Constant.USER_ID);
		String hql="select count(*) from Warehouse t where t.carrierId=:carrierId";
		Map<String,Object> params=new HashMap<String,Object>();
		params.put("carrierId", carrierId);
		
		Long count =warehouseDao.count(hql, params);
		return count.intValue();
	}
	
}
