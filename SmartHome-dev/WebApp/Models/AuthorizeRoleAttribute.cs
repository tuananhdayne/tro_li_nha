using DAO.Context;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.Filters;

namespace WebApp.Models;

public class AuthorizeRoleAttribute : Attribute, IAuthorizationFilter
{
    private readonly string _requiredRole;

    public AuthorizeRoleAttribute(string requiredRole)
    {
        _requiredRole = requiredRole;
    }

    public void OnAuthorization(AuthorizationFilterContext context)
    {
        var dbContext = (SmartHomeContext)context.HttpContext.RequestServices.GetService(typeof(SmartHomeContext))!;
        var userId = context.HttpContext.User.FindFirst("userId")?.Value;

        if (userId == null || !dbContext.HouseMembers.Any(hm => hm.UserID == userId && hm.Role == _requiredRole))
        {
            context.Result = new ForbidResult();
        }
    }
}