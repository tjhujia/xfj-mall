package com.xfj.user.controller;

import com.xfj.commons.result.ResponseData;
import com.xfj.commons.result.ResponseUtil;
import com.xfj.user.IMemberService;
import com.xfj.user.constants.SysRetCodeConstants;
import com.xfj.user.rs.QueryMemberRS;
import com.xfj.user.rs.UpdateMemberRS;
import com.xfj.user.vo.QueryMemberVO;
import com.xfj.user.vo.UpdateMemberVO;
import org.apache.dubbo.config.annotation.Reference;
import org.springframework.web.bind.annotation.*;

/**
 * @Author ZQ
 * @Description 会员中心
 * @Date 2019/11/27 20:23
 **/
@RestController
@RequestMapping("/user")
public class MemberController {

    @Reference(timeout = 3000)
    IMemberService memberService;

    /**
     * 根据ID查询单条会员信息
     *
     * @param id 编号
     * @return
     */
    @GetMapping("/member/{id}")
    public ResponseData searchMemberById(@PathVariable(name = "id") long id) {
        QueryMemberVO request = new QueryMemberVO();
        request.setUserId(id);
        QueryMemberRS queryMemberResponse = memberService.queryMemberById(request);
        if (!queryMemberResponse.getCode().equals(SysRetCodeConstants.SUCCESS.getCode())) {
            return new ResponseUtil<>().setErrorMsg(queryMemberResponse.getMsg());
        }
        return new ResponseUtil<>().setData(queryMemberResponse);
    }

    /**
     * 会员信息更新
     *
     * @return
     */
    @PutMapping("member")
    public ResponseData updateUser(@RequestBody UpdateMemberVO request) {
        UpdateMemberRS response = memberService.updateMember(request);
        if (response.getCode().equals(SysRetCodeConstants.SUCCESS.getCode())) {
            return new ResponseUtil().setData(null);
        }
        return new ResponseUtil().setErrorMsg(response.getMsg());
    }
}

