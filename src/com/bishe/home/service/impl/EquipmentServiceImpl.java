package com.bishe.home.service.impl;

import java.util.LinkedHashMap;

import javax.annotation.Resource;

import net.sf.json.JSONObject;

import org.springframework.stereotype.Service;

import com.baidu.yun.push.exception.PushClientException;
import com.baidu.yun.push.exception.PushServerException;
import com.bishe.home.dao.EquipmentDao;
import com.bishe.home.dao.OperationLogDao;
import com.bishe.home.dao.SceneDao;
import com.bishe.home.domain.Device;
import com.bishe.home.domain.Message;
import com.bishe.home.domain.PublishMsg;
import com.bishe.home.entity.Equipment;
import com.bishe.home.entity.OperationLog;
import com.bishe.home.entity.QueryResult;
import com.bishe.home.entity.Scene;
import com.bishe.home.service.EquipmentService;
import com.bishe.home.service.Subject;
import com.bishe.home.util.GetDateUtil;
import com.bishe.home.util.GetSessionUtil;
import com.bishe.home.util.GetUserUtil;
@Service("equipmentService")
public class EquipmentServiceImpl implements EquipmentService{

	@Resource(name="equipmentDao")
	private EquipmentDao equipmentDao;
	
	@Resource(name="sceneDao")
	private SceneDao sceneDao;

	@Resource(name="operationLogDao")
	private OperationLogDao operationLogDao;
	
	
	@Override
	public QueryResult<Equipment> findByPage(int page, int row,Integer sceneId) {
		int firstResult = (page-1)* row;
		int maxResult = page*row;
		LinkedHashMap<String, String> orderby = new LinkedHashMap<String, String>();
		orderby.put("id", "asc");	
		QueryResult<Equipment> scrollData  = null;
		if(sceneId==0){
			String where = "userid=?";
			Object[] params = new Object[]{GetUserUtil.getUser().getId()};
			scrollData = equipmentDao.getScrollData(firstResult, maxResult, where, params, orderby);
		}else{
			String where = "userid=? and sceneId=?";
			Object[] params = new Object[]{GetUserUtil.getUser().getId(),sceneId};
			scrollData = equipmentDao.getScrollData(firstResult, maxResult, where, params, orderby);
		}
		for(Equipment eq:scrollData.getRows()){
			eq.setSceneId(sceneId);
		}
		return scrollData;
		
	}

	@Override
	public void save(Equipment equipment) {
		Scene scene = sceneDao.find(equipment.getSceneId());
		equipment.setScene(scene);
		equipmentDao.save(equipment);
	}

	@Override
	public Equipment findById(Integer id) {
		return equipmentDao.findById(id);
	}

	@Override
	public void update(Equipment equipment) {
		equipmentDao.updateAttribute(equipment);
	}

	@Override
	public void deleteById(Integer id) {
		equipmentDao.deleteById(id);
	}

	@Override
	public QueryResult<Equipment> findAll() {
		Object[] ids = new Object[]{GetUserUtil.getUser().getId()};
		return equipmentDao.getScrollData("userId=?",ids);
	}

	@Override
	public QueryResult<Equipment> findBySceneId(Integer sceneId) {
		
		if(sceneId==0){
			return this.findAll();
			
		}else{
			String where = "userId=? and scene.id=?";
			Object[] params = new Object[]{GetUserUtil.getUser().getId(),sceneId};
			LinkedHashMap<String, String> orderby = new LinkedHashMap<String, String>();
			orderby.put("id", "asc");		
			return equipmentDao.getScrollData(-1, -1, where, params, orderby);
		}
	}

	@Override
	public OperationLog changeState(Equipment cEquipment) {
		if(cEquipment.getState()==1){
			equipmentDao.turnOn(cEquipment.getId());
		}else{
			equipmentDao.turnOff(cEquipment.getId());
		}
		OperationLog log = new OperationLog();
		Equipment equipment = equipmentDao.findById(cEquipment.getId());
		log.setEquipment(equipment);
		log.setScene(equipment.getScene());
		log.setOpTime(GetDateUtil.getDate());
		log.setUserId(GetUserUtil.getUser().getId());
		if(cEquipment.getState()==1){
			log.setOperation(equipment.getName()+"打开");
		}else{
			log.setOperation(equipment.getName()+"关闭");
		}
		log.setType(0);
		if(equipment.getIsRemind()==1){
			log.setType(1);
//			Remind remind = new Remind();
//			remind.setUserId(GetUserUtil.getUser().getId());
//			remind.setOpTime(GetDateUtil.getDate());
//			remind.setTitle("智能家居");
//			remind.setOperation(equipment.getName()+"被打开。");
//			remind.setEquipment(equipment);
//			remind.setScene(equipment.getScene());
			if(GetSessionUtil.getSession().getAttribute("channelId")!=null&&GetSessionUtil.getSession().getAttribute("userId")!=null){
//				Device device = new Device((Long)GetSessionUtil.getSession().getAttribute("channelId"),(String)GetSessionUtil.getSession().getAttribute("userId"));
//				this.attach(device);
//				if(cEquipment.getState()==1){
//					this.nodifyObservers(new Message("安居",equipment.getName()+"被打开。"));
//				}else{
//					this.nodifyObservers(new Message("安居",equipment.getName()+"被关闭。"));
//				}
//				this.detach(device);
				PublishMsg psh = new PublishMsg((Long)GetSessionUtil.getSession().getAttribute("channelId"),(String)GetSessionUtil.getSession().getAttribute("userId"));
				JSONObject notification = null;
				if(cEquipment.getState()==1){
					notification = JSONObject.fromObject(new Message("安居",equipment.getName()+"被打开。"));
				}else{
					notification = JSONObject.fromObject(new Message("安居",equipment.getName()+"被关闭。"));
				}
				try {
					psh.push(notification);
				} catch (PushClientException | PushServerException e) {
					e.printStackTrace();
				}
			}
		}
		operationLogDao.save(log);
		
		return log;
	}

	/*@Override
	public void turnOff(Integer equipmentId) {
		equipmentDao.turnOff(equipmentId);
		OperationLog log = new OperationLog();
		Equipment equipment = equipmentDao.findById(equipmentId);
		log.setEquipment(equipment);
		log.setScene(equipment.getScene());
		log.setOpTime(GetDateUtil.getDate());
		log.setUserId(GetUserUtil.getUser().getId());
		log.setOperation(equipment.getName()+"关闭");
		log.setType(0);
		if(equipment.getIsRemind()==1){
//			Remind remind = new Remind();
//			remind.setUserId(GetUserUtil.getUser().getId());
//			remind.setOpTime(GetDateUtil.getDate());
//			remind.setTitle("智能家居");
//			remind.setOperation(equipment.getName()+"被关闭。");
//			remind.setEquipment(equipment);
//			remind.setScene(equipment.getScene());
//			remindDao.save(remind);
			log.setType(1);
			Device device = new Device((Long)GetSessionUtil.getSession().getAttribute("channelId"),(String)GetSessionUtil.getSession().getAttribute("userId"));
			this.attach(device);
			this.nodifyObservers(new Message("安居",equipment.getName()+"被关闭。"));
		}
		operationLogDao.save(log); 
	}*/
	
	
	public void chengeTo(Double value,Integer equipmentId){
		
		equipmentDao.changeTo(value, equipmentId);
		OperationLog log = new OperationLog();
		Equipment equipment = equipmentDao.findById(equipmentId);
		log.setEquipment(equipment);
		log.setScene(equipment.getScene());
		log.setOpTime(GetDateUtil.getDate());
		log.setUserId(GetUserUtil.getUser().getId());
		log.setOperation(equipment.getName()+"变化到 "+value);
		log.setType(0);
		if(equipment.getIsRemind()==1){
			log.setType(1);
			Device device = new Device((Long)GetSessionUtil.getSession().getAttribute("channelId"),(String)GetSessionUtil.getSession().getAttribute("userId"));
//			this.attach(device);
//			this.nodifyObservers(new Message("安居",equipment.getName()+"变化到 "+value));
		}
		operationLogDao.save(log); 
	}
	
	
	@Override
	public void addDefaultEq(Integer userId) {
		Equipment temperature = new Equipment();
		temperature.setName("温度");
		temperature.setValue(22.0);
		temperature.setUserId(userId);
		temperature.setType(1);
		
		Equipment water = new Equipment();
		water.setName("水");
		water.setUserId(userId);
		water.setType(2);
		
		Equipment electric = new Equipment();
		electric.setName("电");
		electric.setUserId(userId);
		electric.setType(3);
		
		Equipment gas = new Equipment();
		gas.setName("煤气");
		gas.setUserId(userId);
		gas.setType(4);
		
		equipmentDao.save(temperature);
		equipmentDao.save(water);
		equipmentDao.save(electric);
		equipmentDao.save(gas);
		
	}

	@Override
	public QueryResult<Equipment> findTemperature() {
		String where = "userId=? and type=?";
		Object[] params = new Object[]{GetUserUtil.getUser().getId(),1};
		return equipmentDao.getScrollData(-1, -1, where, params);
		
	}
	

}
