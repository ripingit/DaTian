package cn.edu.bjtu.controller;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import cn.edu.bjtu.bean.page.OrderBean;
import cn.edu.bjtu.service.CarService;
import cn.edu.bjtu.service.CitylineService;
import cn.edu.bjtu.service.CommentService;
import cn.edu.bjtu.service.CompanyService;
import cn.edu.bjtu.service.DriverService;
import cn.edu.bjtu.service.GoodsInfoService;
import cn.edu.bjtu.service.LinetransportService;
import cn.edu.bjtu.service.OrderService;
import cn.edu.bjtu.service.ResponseService;
import cn.edu.bjtu.util.Constant;
import cn.edu.bjtu.util.JSON;
import cn.edu.bjtu.util.PageUtil;
import cn.edu.bjtu.util.UploadFile;
import cn.edu.bjtu.util.UploadPath;
import cn.edu.bjtu.vo.Carinfo;
import cn.edu.bjtu.vo.Carrierinfo;
import cn.edu.bjtu.vo.Cityline;
import cn.edu.bjtu.vo.Comment;
import cn.edu.bjtu.vo.Driverinfo;
import cn.edu.bjtu.vo.Linetransport;
import cn.edu.bjtu.vo.OrderCarrierView;
import cn.edu.bjtu.vo.Orderform;

import com.alibaba.fastjson.JSONArray;

/**
 * 
 * @author RussWest0
 *
 */
@Controller
public class OrderController {

	@Resource
	OrderService orderService;

	@Resource(name = "carServiceImpl")
	CarService carService;

	@Resource
	LinetransportService linetransportService;
	@Resource
	CitylineService citylineService;
	@Autowired
	CompanyService companyService;
	@Autowired
	CommentService commentService;
	@Autowired
	GoodsInfoService goodsInfoService;
	@Autowired
	ResponseService responseService;
	
	@Autowired
	DriverService driverService;
	
	ModelAndView mv = new ModelAndView();

	@RequestMapping("/sendorderinfo")
	/**
	 * 我提交的订单信息
	 * @return
	 */
	public String getAllSendOrderInfo(HttpSession session) {
		return "mgmt_d_order_s";
	}
	/**
	 * 跳转到订单列表页面
	 * @param session
	 * @return
	 */
	@RequestMapping("turnToOrderPage")
	public String orderPage(HttpSession session){
		Integer userKind=(Integer)session.getAttribute(Constant.USER_KIND);
		if(userKind==2){//个人用户
			return "mgmt_d_order_s";
		}else if(userKind==3){//企业用户
			return "mgmt_d_order_r";
		}
		return "index";
	}
	
	/**
	 * u获取用户提交的订单
	 * @param session
	 * @return
	 */
	@RequestMapping(value="getUserSendOrderAjax",produces="text/html;charset=UTF-8")
	@ResponseBody
	public String getUserSendOrder(HttpSession session,PageUtil pageUtil,Orderform order){
		JSONArray jsonArray=orderService.getUserSendOrder(session,pageUtil,order);
		
		return jsonArray.toString();
		
	}

	/**
	 * 我提交的订单-总记录以
	 * @param session
	 * @return
	 */
	@ResponseBody
	@RequestMapping("getUseSendOrderTotalRowsAjax")
	public Integer getUserSendOrderTotalRows(HttpSession session,Orderform order){
		return orderService.getUserSendOrderTotalRows(session,order);
	}

	@RequestMapping("/recieveorderinfo")
	/**
	 * 我收到的订单信息
	 * @return
	 */
	public String  getAllRecieveOrderInfo(HttpServletRequest request,
			HttpServletResponse response) {
		return "mgmt_d_order_r";
	}
	
	/**
	 * 我收到的订单
	 * @param session
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value="getUserRecieveOrderAjax",produces="text/html;charset=UTF-8")
	public String getUserRecieveOrder(HttpSession session,PageUtil pageUtil,Orderform order){
		JSONArray jsonArray=orderService.getUserRecieveOrder(session,pageUtil,order);
		return jsonArray.toString();
	}
	
	/**
	 * 我收到的订单-总记录数
	 * @param session
	 * @return
	 */
	@ResponseBody
	@RequestMapping("getUserRecieveOrderTotalRowsAjax")
	public Integer getUserRevieveOrderTotalRows(HttpSession session,Orderform order){
		return orderService.getUserRecieveOrderTotalRows(session,order);
	}

	/**
	 * 提交订单详情
	 * @param id
	 * @return
	 */
	@RequestMapping("/sendorderdetail")
	public ModelAndView getSendOrderDetail(@RequestParam String id) {
		OrderCarrierView sendorderdetail = orderService.getSendOrderDetail(id);
		mv.addObject("sendorderdetail", sendorderdetail);

		mv.setViewName("mgmt_d_order_s4");

		return mv;
	}

	/**
	 * 收到订单详情
	 * @param id
	 * @return
	 */
	@RequestMapping("/recieveorderdetail")
	public ModelAndView getAllRecieveOrderDetail(@RequestParam String id) {
		Orderform recieveorderdetail = orderService.getRecieveOrderDetail(id);
		mv.addObject("recieveorderdetail", recieveorderdetail);
		mv.setViewName("mgmt_d_order_r3");

		return mv;
	}

	/**
	 * 获取更新订单表单
	 * @param orderId
	 * @return
	 */
	@RequestMapping("getUpdateOrderForm")
	public ModelAndView getUpdateOrderForm(String orderId) {

		OrderCarrierView orderCarrierView = orderService
				.getOrderByOrderId(orderId);// 从视图查

		mv.addObject("orderinfo", orderCarrierView);
		mv.setViewName("mgmt_d_order_s3");
		return mv;
	}

	/**
	 * 获取受理表单
	 * @param orderid
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping("acceptOrderForm")
	public ModelAndView getAcceptOrderForm(String orderid,
			HttpServletRequest request, HttpServletResponse response) {

		// 需要查出公司司机列表 add by RussWest0 at 2015年6月7日,下午7:56:32 
		String carrierId = (String) request.getSession().getAttribute(Constant.USER_ID);
		List<Driverinfo> driverList=driverService.getAllDriver(carrierId);
		mv.addObject("driverList",driverList);
		// 需要获取车牌号和司机名
		mv.addObject("orderId", orderid);

		mv.setViewName("mgmt_d_order_r2");
		return mv;
	}

	/**
	 * 受理操作
	 * @param orderid
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping("acceptOrder")
	public String acceptOrder(String orderid, HttpServletRequest request,
			HttpServletResponse response,String driver) {

		// 需要更新订单的司机列表，并且修改订单状态为已受理(待收货)
		// 需要重定向,用来更新页面
		//获取到司机，但是未作处理 add by RussWest0 at 2015年6月7日,下午8:03:50 
		orderService.acceptOrder(orderid);
		return "redirect:recieveorderinfo";
	}

	/**
	 * 获取签单上传表单
	 * @param orderid
	 * @return
	 */
	@RequestMapping("getSignBillForm")
	public ModelAndView getSignBillForm(String orderid) {
		// 上传图片，添加实际运费，修改订单状态为待确认
		// 需要再页面上显示合同规定运费和预期运费
		// 上传图片未实现
		float expectedMoney = orderService.getExpectedMoney(orderid);
		// System.out.println("签单上传+orderid+" + orderid);
		mv.addObject("expectedPrice", expectedMoney);
		mv.addObject("orderId", orderid);
		mv.setViewName("mgmt_d_order_r6");
		return mv;
	}

	/**
	 * 签单上传
	 * @param file
	 * @param orderid
	 * @param actualPrice
	 * @param explainReason
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping("signBill")
	public String SignBill(@RequestParam(required = false) MultipartFile file,String orderid, float actualPrice,
			String explainReason, HttpServletRequest request,
			HttpServletResponse response) {
		String carrierId = (String) request.getSession().getAttribute(Constant.USER_ID);

		//保存文件
		String fileLocation=UploadFile.uploadFile(file, carrierId, "signBill");
		orderService.signBill(orderid, actualPrice,
				explainReason,fileLocation);

		return "redirect:recieveorderinfo";
	}

	/**
	 * 获取确认收货表单
	 * @param orderid
	 * @return
	 */
	@RequestMapping("getConfirmForm")
	public ModelAndView getConfirmForm(String orderid) {
		// 跳到确认收货页面
		// 需要规定费用，实际费用，说明
		Orderform order = orderService.getOrderInfo(orderid);
		Float expectedPrice = order.getExpectedPrice();
		Float actualPrice = order.getActualPrice();
		String explianReason = order.getExplainReason();
		mv.addObject("orderId", orderid);
		mv.addObject("expectedPrice", expectedPrice);
		mv.addObject("actualPrice", actualPrice);
		mv.addObject("explainReason", explianReason);
		mv.setViewName("mgmt_d_order_s5");
		return mv;
	}

	@RequestMapping("confirm")
	public String confirm(String orderid, HttpServletRequest request,
			HttpServletResponse response) {
		// 修改订单为待评价
		boolean flag = orderService.confirmCargo(orderid);
		mv.addObject("orderId", orderid);


		return "redirect:turnToOrderPage";
	}

	@RequestMapping("getCommentForm")
	/**
	 * 获取评价页面
	 * @param orderid
	 * @return
	 */
	public ModelAndView getCommentForm(String orderid,String ordernum)
	{

		mv.addObject("orderId", orderid);
		mv.addObject("orderNum", ordernum);
		mv.setViewName("mgmt_d_order_s8");
		return mv;
	}

	@RequestMapping("comment")
	public ModelAndView comment(String orderid, int serviceAttitude,
			int transportEfficiency, int cargoSafety, int totalMoney,
			HttpServletRequest request, HttpServletResponse response) {
		//FIXME
		// 修改订单状态为已完成
		// 存储评价内容
		// 评价页面错误

		return mv;
	}

	/**
	 * 获取更新订单页面
	 * 
	 * @param orderid
	 * @return
	 */
	@RequestMapping(value = "updateOrder")
	public ModelAndView getUpdateOrderPage(@RequestParam("orderid") String orderid,
			HttpServletRequest request) {
		OrderCarrierView orderInfo = orderService.getOrderByOrderId(orderid);
		mv.addObject("orderInfo", orderInfo);
		mv.setViewName("mgmt_d_order_s3");
		return mv;
	}

	
	/**
	 * 更新订单
	 * @param session
	 * @param orderBean
	 * @return
	 */
	@RequestMapping("doUpdate")
	public String updateOrder(HttpSession session,OrderBean orderBean){
		
		orderService.updateOrder(session, orderBean);
		return "redirect:turnToOrderPage";
		
	}

	/**
	 * 取消订单
	 * 
	 * @param orderid
	 * @return
	 */
	@RequestMapping(value = "cancelOrder")
	public ModelAndView cancelOrder(HttpServletRequest request,
			HttpServletResponse response, String orderid) {
		OrderCarrierView orderInfo = orderService.getOrderByOrderId(orderid);
		mv.addObject("orderInfo", orderInfo);
		mv.setViewName("mgmt_d_order_s9");
		return mv;
	}

	/**
	 * 取消订单
	 * 
	 * @param orderid
	 * @return
	 */
	@RequestMapping(value = "getOrderCancelOrder")
	public ModelAndView getOrderCancelOrder(HttpServletRequest request,
			HttpServletResponse response, String orderid) {
		OrderCarrierView orderInfo = orderService.getOrderByOrderId(orderid);
		mv.addObject("orderInfo", orderInfo);
		mv.setViewName("mgmt_d_order_r7");
		return mv;
	}

	/**
	 * 取消订单
	 * 
	 * @param orderid
	 * @return
	 */
	@RequestMapping(value = "doCancel", method = RequestMethod.POST)
	public String doCancel(HttpServletRequest request,
			HttpServletResponse response, @RequestParam String cancelReason,
			String orderid) {
		boolean flag = orderService.cancel(cancelReason, orderid);
		return "redirect:turnToOrderPage";
	}

	/**
	 * 取消订单
	 * 
	 * @param orderid
	 * @return
	 */
	@RequestMapping(value = "getOrderDoCancel", method = RequestMethod.POST)
	public String getOrderDoCancel(HttpServletRequest request,
			HttpServletResponse response, @RequestParam String cancelReason,
			String orderid) {
		 orderService.cancel(cancelReason, orderid);
		return "redirect:recieveorderinfo";
	}

	@RequestMapping(value = "orderDetail")
	/**
	 * 
	 * 
	 * @param orderid
	 * @return
	 */
	public ModelAndView orderDetail(HttpServletRequest request,
			HttpServletResponse response, String orderid) {
		OrderCarrierView orderInfo = orderService.getOrderByOrderId(orderid);
		mv.addObject("orderInfo", orderInfo);
		mv.setViewName("mgmt_d_order_s4");
		return mv;
	}

	/**
	 * 
	 * 
	 * @param orderid
	 * @return
	 */
	@RequestMapping(value = "orderDetailWaitToReceive")
	public ModelAndView orderDetailWaitToReceive(HttpServletRequest request,
			HttpServletResponse response, String orderid) {
		OrderCarrierView orderInfo = orderService.getOrderByOrderId(orderid);
		mv.addObject("orderInfo", orderInfo);
		mv.setViewName("mgmt_d_order_s6");
		return mv;
	}

	/**
	 * 
	 * 
	 * @param orderid
	 * @return
	 */
	@RequestMapping(value = "orderDetailAlreadyCancel")
	public ModelAndView orderDetailAlreadyCancel(HttpServletRequest request,
			HttpServletResponse response, String orderid) {
		OrderCarrierView orderInfo = orderService.getOrderByOrderId(orderid);
		mv.addObject("orderInfo", orderInfo);
		mv.setViewName("mgmt_d_order_s4a");
		return mv;
	}

	/**
	 * 
	 * 订单完成后查看操作
	 * @param orderid
	 * @return
	 */
	@RequestMapping(value = "orderDetailFinish")
	public ModelAndView orderDetailFinish(HttpServletRequest request,
			HttpServletResponse response, String orderid) {
		OrderCarrierView orderInfo = orderService.getOrderByOrderId(orderid);
		mv.addObject("orderInfo", orderInfo);
		//页面需要评价信息 add by RussWest0 at 2015年6月7日,下午4:04:16 
		Comment comment=commentService.getCommentByOrderId(orderid);
		mv.addObject("comment", comment);
		mv.setViewName("mgmt_d_order_s6a");
		return mv;
	}

	@RequestMapping(value = "orderDetailComment")
	public ModelAndView orderDetailComment(HttpServletRequest request,
			HttpServletResponse response, String orderid) {
		OrderCarrierView orderInfo = orderService.getOrderByOrderId(orderid);
		mv.addObject("orderInfo", orderInfo);
		mv.setViewName("mgmt_d_order_s6b");
		return mv;
	}

	/**
	 * 
	 * 
	 * @param orderid
	 * @return
	 */
	@RequestMapping(value = "getOrderDetail")
	public ModelAndView getOrderDetail(HttpServletRequest request,
			HttpServletResponse response, String orderid) {
		OrderCarrierView orderInfo = orderService.getOrderByOrderId(orderid);
		mv.addObject("orderInfo", orderInfo);
		mv.setViewName("mgmt_d_order_r3");
		return mv;
	}

	/**
	 * 
	 * 
	 * @param orderid
	 * @return
	 */
	@RequestMapping(value = "getOrderDetailCancel")
	public ModelAndView getOrderDetailCancel(HttpServletRequest request,
			HttpServletResponse response, String orderid) {
		OrderCarrierView orderInfo = orderService.getOrderByOrderId(orderid);
		mv.addObject("orderInfo", orderInfo);
		mv.setViewName("mgmt_d_order_r3a");
		return mv;
	}

	/**
	 * 
	 * 
	 * @param orderid
	 * @return
	 */
	@RequestMapping(value = "getOrderDetailWaitToReceive")
	public ModelAndView getOrderDetailWaitToReceive(HttpServletRequest request,
			HttpServletResponse response, String orderid) {
		OrderCarrierView orderInfo = orderService.getOrderByOrderId(orderid);
		mv.addObject("orderInfo", orderInfo);
		mv.setViewName("mgmt_d_order_r4");
		return mv;
	}
	
	/**
	 * 
	 * 
	 * @param orderid
	 * @return
	 */
	@RequestMapping(value = "getOrderDetailWaitToConfirm")
	public ModelAndView getOrderDetailWaitToConfirm(HttpServletRequest request,
			HttpServletResponse response, String orderid) {
		OrderCarrierView orderInfo = orderService.getOrderByOrderId(orderid);
		mv.addObject("orderInfo", orderInfo);
		mv.setViewName("mgmt_d_order_r4a");
		return mv;
	}

	/**
	 * 
	 * 承运方-我收到的订单-已完成-查看
	 * @param orderid
	 * @return
	 */
	@RequestMapping(value = "getOrderDetailFinish")
	public ModelAndView getOrderDetailFinish(HttpServletRequest request,
			HttpServletResponse response, String orderid) {
		OrderCarrierView orderInfo = orderService.getOrderByOrderId(orderid);
		mv.addObject("orderInfo", orderInfo);
		//页面需要评价信息
		Comment comment=commentService.getCommentByOrderId(orderid);
		mv.addObject("comment",comment);
		mv.setViewName("mgmt_d_order_r4b");
		return mv;
	}

	@RequestMapping("getOrderWaitToConfirmUpdate")
	public ModelAndView getOrderWaitToConfirmUpdate(String orderid) {

		// 需要再页面上显示合同规定运费和预期运费,实际运费,原因
		// 上传图片未实现

		OrderCarrierView orderInfo = orderService.getOrderByOrderId(orderid);
		mv.addObject("orderInfo", orderInfo);
		mv.setViewName("mgmt_d_order_r6a");
		return mv;
	}

	/**
	 * 承运方签单上传后的跟新
	 * @param orderid
	 * @param actualPrice
	 * @param explainReason
	 * @param request
	 * @param response
	 * @param file
	 * @return
	 */
	@RequestMapping("updateSignBill")
	public String updateSignBill(String orderid,
			float actualPrice, String explainReason,
			HttpServletRequest request, HttpServletResponse response,@RequestParam(required = false) MultipartFile file) {
		String carrierId = (String) request.getSession().getAttribute(Constant.USER_ID);

		String fileLocation=UploadFile.uploadFile(file, carrierId, "signBill");
		orderService.updateSignBill(orderid,
				actualPrice, explainReason,fileLocation);
		return "redirect:recieveorderinfo";
	}

	@RequestMapping("getneworderform")
	/**
	 * 获取创建订单表单
	 * @return
	 */
	public ModelAndView getNewOrderForm(@RequestParam String carrierid,
			@RequestParam(required=false) String resourceId,@RequestParam int flag) {
		// 需要取出承运方公司名称
		//flag和resourceType中标识1为干线，2为城市，3为车辆,4为公司
		int resourceType = 0;
		if(flag==4){//从公司页面提交订单
			Carrierinfo carrierInfo=companyService.getCompanyById(carrierid);
			mv.addObject("carrierInfo", carrierInfo);
			mv.addObject("carrierId", carrierid);
			
			mv.setViewName("mgmt_d_order_s2a");
			return mv;
		}
		if(flag==1){//从干线资源提交订单
			Linetransport linetransportInfo = linetransportService
					.getLinetransportInfo(resourceId);
			resourceType = 1;
			mv.addObject("resourceType", resourceType);
			mv.addObject("linetransportInfo", linetransportInfo);
		}
		if(flag==2){// 从城市配送提交订单
			Cityline citylineInfo = citylineService.getCitylineInfo(resourceId);
			resourceType = 2;
			mv.addObject("resourceType", resourceType);
			mv.addObject("citylineInfo", citylineInfo);
		}
		if(flag==3){//从车辆资源提交订单
			Carinfo carInfo = carService.getCarInfo(resourceId);
			resourceType = 3;
			mv.addObject("resourceType", resourceType);
			mv.addObject("carInfo", carInfo);
		}
		Carrierinfo company=companyService.getCompanyById(carrierid);
		mv.addObject("companyName", company.getCompanyName());
		mv.addObject("carrierId", carrierid);
		mv.setViewName("mgmt_d_order_s2");
		return mv;
	}

	
	
	/**
	 * 新建订单
	 * @param session
	 * @param orderBean
	 * @return
	 */
	@RequestMapping("createneworder")
	public String createNewOrder(HttpSession session,OrderBean orderBean){
		JSON json=new JSON();
		boolean flag=orderService.createOrder(session,orderBean);
		if(flag==true){
			json.setMsg("sucess");
			json.setSuccess(true);
		}else{
			json.setMsg("fail");
			json.setSuccess(false);
		}
		
		return "redirect:turnToOrderPage";
	}
	
	
	/**
	 * 从我的货物栏下订单
	 * @param session
	 * @param orderBean
	 * @return
	 */
	@RequestMapping("createOrderFromCargo")
	public String createOrderFromCargo(HttpSession session,OrderBean orderBean){
		boolean flag=orderService.createOrder(session,orderBean);
		String goodsId=orderBean.getGoodsId();
		String responseId=orderBean.getResponseId();
		String carrierId=orderBean.getCarrierId();
		if (flag == true) {
			//反馈表修改状态
			responseService.confirmResponse(responseId,carrierId,goodsId);//修改确认反馈信息为已确认，其它反馈信息为已取消状态
			//货物表修改状态
			goodsInfoService.confirmResponse(goodsId);
			return "redirect:turnToOrderPage";
		}
		return "redirect:mgmt_d_order_s";
	}

}
